package com.rassix.randomNumberGenerator.service;

import com.rassix.randomNumberGenerator.constant.GamePhase;
import com.rassix.randomNumberGenerator.controller.dto.AddPlayerRequest;
import com.rassix.randomNumberGenerator.exception.PlayerExistsException;
import com.rassix.randomNumberGenerator.repository.GameRepository;
import com.rassix.randomNumberGenerator.repository.model.Game;
import com.rassix.randomNumberGenerator.service.dto.Notification;
import com.rassix.randomNumberGenerator.service.dto.WinningPlayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rassix.randomNumberGenerator.repository.model.Game.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private GameService gameService;

    @Test
    void createGame_ifGameIsCreated_returnsGameId() {
        Instant bettingEndTime = Instant.now().plus(20, ChronoUnit.SECONDS);
        Integer winningNumber = 5;

        ArgumentCaptor<Game> captor = ArgumentCaptor.forClass(Game.class);

        when(gameRepository.createGame(captor.capture())).thenReturn("random-game-id");

        String gameId = gameService.createGame(bettingEndTime, winningNumber);

        Game capturedGame = captor.getValue();

        Game expectedCapture = builder()
            .betEndingTime(bettingEndTime)
            .winningNumber(5)
            .gamePhase(GamePhase.BETTING_PHASE)
            .build();

        assertThat(capturedGame).usingRecursiveComparison().isEqualTo(expectedCapture);
        assertThat(gameId).isEqualTo("random-game-id");
    }

    @Test
    void changeGamePhase_ifNewPhaseResultsPhase_MessageSentOut() {
        String gameId = "random-game-id";

        gameService.changeGamePhase(gameId, GamePhase.RESULTS_PHASE);

        verify(gameRepository).changeGamePhase("random-game-id", GamePhase.RESULTS_PHASE);
        verify(notificationService).notifyBettingEnd();
    }

    @ParameterizedTest
    @EnumSource(
        value = GamePhase.class,
        names = {"BETTING_PHASE", "ENDED"}
    )
    void changeGamePhase_ifNewPhaseNotResultsPhase_MessageNotSentOut(GamePhase gamePhase) {
        String gameId = "random-game-id";

        gameService.changeGamePhase(gameId, gamePhase);

        verify(gameRepository).changeGamePhase("random-game-id", gamePhase);
        verify(notificationService, never()).notifyBettingEnd();
    }

    @Test
    void getGameDetails_returnGame_ifMethodCalled() {
        String gameId = "random-game-id";
        Instant betEndTime = Instant.now().plus(10, ChronoUnit.SECONDS);

        Game expectedResult = builder()
            .gamePhase(GamePhase.ENDED)
            .winningNumber(2)
            .betEndingTime(betEndTime)
            .players(Map.of("Dave", Player.builder().betAmount(new BigDecimal("4")).guessedNumber(4).build()))
            .build();

        when(gameRepository.getGame(gameId)).thenReturn(
            builder()
                .gamePhase(GamePhase.ENDED)
                .winningNumber(2)
                .betEndingTime(betEndTime)
                .players(Map.of("Dave", Player.builder().betAmount(new BigDecimal("4")).guessedNumber(4).build()))
                .build()
        );

        Game gameDetails = gameService.getGameDetails(gameId);

        verify(gameRepository).getGame(gameId);
        assertThat(gameDetails).usingRecursiveComparison().isEqualTo(expectedResult);
    }

    @Test
    void addPlayer_playerIsAdded_ifMethodIsCalled() throws PlayerExistsException {
        String gameId = "still-random-game-id";
        AddPlayerRequest addPlayerRequest = AddPlayerRequest.builder()
            .guessedNumber(4)
            .bid(new BigDecimal("6.53"))
            .username("Dave")
            .build();

        gameService.addPlayer(gameId, addPlayerRequest);

        ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);

        verify(gameRepository).addPlayer(eq(gameId), eq("Dave"), playerCaptor.capture());

        Player expectedResult = Player.builder().betAmount(new BigDecimal("6.53")).guessedNumber(4).build();

        assertThat(playerCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedResult);
    }

    @Test
    void getActiveGame_ifActiveGameExists_returnsDetailsAboutIt() {
        String game1Id = "game1Id";
        Instant game1BettingEndTime = Instant.now().minus(20, ChronoUnit.MINUTES);
        Game game1 = Game.builder().gamePhase(GamePhase.ENDED).betEndingTime(game1BettingEndTime).build();

        String game2Id = "game2Id";
        Instant game2BettingEndTime = Instant.now().minus(5, ChronoUnit.MINUTES);
        Game game2 = Game.builder().gamePhase(GamePhase.ENDED).betEndingTime(game2BettingEndTime).build();

        String game3Id = "game3Id";
        Instant game3BettingEndTime = Instant.now().plus(10, ChronoUnit.SECONDS);
        Game game3 = Game.builder().gamePhase(GamePhase.BETTING_PHASE).betEndingTime(game3BettingEndTime).build();

        Map<String, Game> allGames = Map.of(game1Id, game1, game2Id, game2, game3Id, game3);

        when(gameRepository.getAllGames()).thenReturn(allGames);

        Map.Entry<String, Game> activeGame = gameService.getActiveGame();

        Game expectedObject = builder().gamePhase(GamePhase.BETTING_PHASE).betEndingTime(game3BettingEndTime).build();

        assertThat(activeGame.getKey()).isEqualTo("game3Id");
        assertThat(activeGame.getValue()).usingRecursiveComparison().isEqualTo(expectedObject);
    }

    @Test
    void getActiveGame_ifActiveGameDoesntExist_returnNull() {
        String game1Id = "game1Id";
        Instant game1BettingEndTime = Instant.now().minus(20, ChronoUnit.MINUTES);
        Game game1 = Game.builder().gamePhase(GamePhase.ENDED).betEndingTime(game1BettingEndTime).build();

        String game2Id = "game2Id";
        Instant game2BettingEndTime = Instant.now().minus(10, ChronoUnit.MINUTES);
        Game game2 = Game.builder().gamePhase(GamePhase.ENDED).betEndingTime(game2BettingEndTime).build();

        String game3Id = "game3Id";
        Instant game3BettingEndTime = Instant.now().minus(5, ChronoUnit.MINUTES);
        Game game3 = Game.builder().gamePhase(GamePhase.ENDED).betEndingTime(game3BettingEndTime).build();

        Map<String, Game> allGames = Map.of(game1Id, game1, game2Id, game2, game3Id, game3);

        when(gameRepository.getAllGames()).thenReturn(allGames);

        Map.Entry<String, Game> activeGame = gameService.getActiveGame();

        assertThat(activeGame).isNull();
    }

    @Test
    void getWinningPlayers_ifMethodCalled_returnsWinnersAndTheirWinningAmount() {
        String gameId = "testing-id";

        Game game = builder()
            .betEndingTime(Instant.now().minus(50, ChronoUnit.SECONDS))
            .gamePhase(GamePhase.RESULTS_PHASE)
            .winningNumber(5)
            .players(buildPlayerBase())
            .build();

        when(gameRepository.getGame(gameId)).thenReturn(game);

        List<WinningPlayer> winningPlayers = gameService.getWinningPlayers(gameId);

        assertThat(winningPlayers).hasSize(2);

        WinningPlayer expectedWin1 = WinningPlayer.builder().name("Lucy").wonAmount(new BigDecimal("13188.10")).build();
        WinningPlayer expectedWin2 = WinningPlayer.builder().name("Gilead").wonAmount(new BigDecimal("336.60")).build();

        assertThat(winningPlayers.get(0)).usingRecursiveComparison().isEqualTo(expectedWin1);
        assertThat(winningPlayers.get(1)).usingRecursiveComparison().isEqualTo(expectedWin2);
    }

    @Test
    void getUnluckyPlayers_ifCalled_returnsListOfNamesWhoDidntWin() {
        String gameId = "testing-id";

        Game game = builder()
            .betEndingTime(Instant.now().minus(50, ChronoUnit.SECONDS))
            .gamePhase(GamePhase.RESULTS_PHASE)
            .winningNumber(5)
            .players(buildPlayerBase())
            .build();

        when(gameRepository.getGame(gameId)).thenReturn(game);

        List<String> unluckyPlayers = gameService.getUnluckyPlayers(gameId);

        assertThat(unluckyPlayers).containsExactlyInAnyOrder("Arthur", "Roland", "Lilith");
    }

    @Test
    void notifyOfWinners_ifMethodCalled_ensureAllWinnersGetNotifiedAndResultsSentToAll() {
        WinningPlayer winningPlayer1 = WinningPlayer.builder()
            .name("Lucy")
            .wonAmount(new BigDecimal("13188.10"))
            .build();

        WinningPlayer winningPlayer2 = WinningPlayer.builder()
            .name("Gilead")
            .wonAmount(new BigDecimal("336.60"))
            .build();
        List<WinningPlayer> winnersList = List.of(winningPlayer1, winningPlayer2);

        gameService.notifyOfWinners(winnersList);

        ArgumentCaptor<Notification> lucyCaptor = ArgumentCaptor.forClass(Notification.class);
        ArgumentCaptor<Notification> gileadCaptor = ArgumentCaptor.forClass(Notification.class);
        ArgumentCaptor<Notification> globalMessageCaptor = ArgumentCaptor.forClass(Notification.class);

        verify(notificationService).sendMessageToPlayer(eq("Lucy"), lucyCaptor.capture());
        verify(notificationService).sendMessageToPlayer(eq("Gilead"), gileadCaptor.capture());
        verify(notificationService).sendGlobalMessage(globalMessageCaptor.capture());

        assertThat(lucyCaptor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(Notification.builder().messageCode("WIN_NOTIFICATION").messagePayload(winningPlayer1).build());

        assertThat(gileadCaptor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(Notification.builder().messageCode("WIN_NOTIFICATION").messagePayload(winningPlayer2).build());

        assertThat(globalMessageCaptor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(Notification.builder().messageCode("WINNER_LISTING").messagePayload(winnersList).build());
    }

    @Test
    void notifyLoss_ifMethodCalled_messageIsSentToPeopleInList() {
        List<String> notWinNames = List.of("Arthur", "Roland", "Lilith");
        gameService.notifyLoss(notWinNames);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

        verify(notificationService, times(3))
            .sendMessageToPlayer(nameCaptor.capture(), notificationCaptor.capture());

        List<String> capturedNames = nameCaptor.getAllValues();
        List<Notification> capturedNotifications = notificationCaptor.getAllValues();

        assertThat(capturedNames).containsExactlyInAnyOrder("Arthur", "Roland", "Lilith");
        assertThat(capturedNotifications).extracting(x -> x.getMessageCode()).containsOnly("LOSS_NOTIFICATION");
        assertThat(capturedNotifications).extracting(x -> x.getMessagePayload()).containsNull();
    }

    private Map<String, Player> buildPlayerBase() {
        HashMap<String, Player> players = new HashMap<>();

        players.put("Arthur", Player.builder().guessedNumber(2).betAmount(new BigDecimal(233.4)).build());
        players.put("Gilead", Player.builder().guessedNumber(5).betAmount(new BigDecimal(34)).build());
        players.put("Roland", Player.builder().guessedNumber(1).betAmount(new BigDecimal(42.09)).build());
        players.put("Lucy", Player.builder().guessedNumber(5).betAmount(new BigDecimal(1332.131113)).build());
        players.put("Lilith", Player.builder().guessedNumber(3).betAmount(new BigDecimal(5)).build());

        return players;
    }

}