package com.rassix.randomNumberGenerator.service;

import com.rassix.randomNumberGenerator.constant.GamePhase;
import com.rassix.randomNumberGenerator.controller.dto.AddPlayerRequest;
import com.rassix.randomNumberGenerator.exception.PlayerExistsException;
import com.rassix.randomNumberGenerator.repository.GameRepository;
import com.rassix.randomNumberGenerator.repository.model.Game;
import com.rassix.randomNumberGenerator.repository.model.Game.Player;
import com.rassix.randomNumberGenerator.service.dto.Notification;
import com.rassix.randomNumberGenerator.service.dto.WinningPlayer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.rassix.randomNumberGenerator.constant.GamePhase.RESULTS_PHASE;

@Service
@Slf4j
public class GameService {

    private final NotificationService notificationService;
    private final GameRepository gameRepository;

    public GameService(NotificationService notificationService, GameRepository gameRepository) {
        this.notificationService = notificationService;
        this.gameRepository = gameRepository;
    }

    private static final BigDecimal WINNING_COEF = new BigDecimal("9.9");

    public String createGame(Instant betEndingTime, Integer winningNumber) {
        Game game = Game.builder()
                .betEndingTime(betEndingTime)
                .gamePhase(GamePhase.BETTING_PHASE)
                .winningNumber(winningNumber)
                .build();

        String gameId = gameRepository.createGame(game);
        notificationService.notifyNewGameStart(gameId, betEndingTime);
        return gameId;
    }

    public void changeGamePhase(String gameId, GamePhase newPhase) {
        gameRepository.changeGamePhase(gameId, newPhase);

        if (RESULTS_PHASE == newPhase) {
            notificationService.notifyBettingEnd();
        }
    }

    public Game getGameDetails(String gameId) {
        return gameRepository.getGame(gameId);
    }

    public void addPlayer(String gameId, AddPlayerRequest addPlayerRequest) throws PlayerExistsException {
        Player player = Player.builder()
                .betAmount(addPlayerRequest.getBid())
                .guessedNumber(addPlayerRequest.getGuessedNumber())
                .build();

        gameRepository.addPlayer(gameId, addPlayerRequest.getUsername(), player);
    }

    public Map.Entry<String, Game> getActiveGame() {
        Map<String, Game> allGames = gameRepository.getAllGames();

        return allGames.entrySet()
                .stream()
                .filter(x -> GamePhase.ENDED != x.getValue().getGamePhase())
                .findFirst()
                .orElse(null);
    }

    public List<WinningPlayer> getWinningPlayers(String gameId) {
        Game game = getGameDetails(gameId);

        return game.getPlayers()
            .entrySet()
            .stream()
            .filter(x -> game.getWinningNumber() == x.getValue().getGuessedNumber())
            .map(x -> WinningPlayer.builder()
                .name(x.getKey())
                .wonAmount(calculateWonAmount(x.getValue().getBetAmount()))
                .build()
            ).collect(Collectors.toList());
    }

    public List<String> getUnluckyPlayers(String gameId) {
        Game game = getGameDetails(gameId);

        return game.getPlayers()
            .entrySet()
            .stream()
            .filter(x -> game.getWinningNumber() != x.getValue().getGuessedNumber())
            .map(x -> x.getKey())
            .collect(Collectors.toList());
    }

    public void notifyOfWinners(List<WinningPlayer> winningPlayers) {
        winningPlayers.forEach(x -> {
            Notification winnerNotification = Notification.builder()
                .messageCode("WIN_NOTIFICATION")
                .messagePayload(x)
                .build();
            notificationService.sendMessageToPlayer(x.getName(), winnerNotification);
        });

        if (!winningPlayers.isEmpty()) {
            notificationService.sendGlobalMessage(Notification.builder()
                .messageCode("WINNER_LISTING")
                .messagePayload(winningPlayers)
                .build()
            );
        }
    }

    public void notifyLoss(List<String> name) {
        name.forEach(x -> {
            notificationService.sendMessageToPlayer(x, Notification.builder().messageCode("LOSS_NOTIFICATION").build());
        });
    }

    private BigDecimal calculateWonAmount(BigDecimal betAmount) {
        BigDecimal wonAmount = betAmount.multiply(WINNING_COEF).setScale(2, RoundingMode.HALF_EVEN);
        return wonAmount;
    }
}
