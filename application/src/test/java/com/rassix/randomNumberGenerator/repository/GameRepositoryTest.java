package com.rassix.randomNumberGenerator.repository;

import com.rassix.randomNumberGenerator.constant.GamePhase;
import com.rassix.randomNumberGenerator.exception.GameMissingException;
import com.rassix.randomNumberGenerator.exception.PlayerExistsException;
import com.rassix.randomNumberGenerator.repository.model.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.rassix.randomNumberGenerator.repository.model.Game.Player;
import static com.rassix.randomNumberGenerator.repository.model.Game.builder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameRepositoryTest {

    @Spy
    private HashMap<String, Player> playerMock;

    private GameRepository gameRepository;

    @BeforeEach
    void setup() {
        gameRepository = new GameRepository();
    }

    @Test
    void createGame_ifGameIsProvided_newGameStored() {
        Instant bettingEndTime = Instant.now().plus(50, ChronoUnit.SECONDS);

        Game game = builder().gamePhase(GamePhase.BETTING_PHASE)
            .betEndingTime(bettingEndTime)
            .winningNumber(24)
            .build();

        String gameId = gameRepository.createGame(game);

        Map<String, Game> allGames = gameRepository.getAllGames();

        assertThat(allGames).hasSize(1);

        Game expectedResult = builder().gamePhase(GamePhase.BETTING_PHASE)
            .betEndingTime(bettingEndTime)
            .winningNumber(24)
            .players(new HashMap<>())
            .build();

        assertThat(allGames.get(gameId))
            .usingRecursiveComparison()
            .isEqualTo(expectedResult);
    }

    @Test
    void addPlayer_ifMethodCalled_PersonIsAddedToList() throws PlayerExistsException {
        Game game = builder().gamePhase(GamePhase.BETTING_PHASE)
            .betEndingTime(Instant.now().plus(23, ChronoUnit.MINUTES))
            .winningNumber(24)
            .build();

        String gameId = gameRepository.createGame(game);

        gameRepository.addPlayer(
                gameId,
                "Dave",
                Player.builder().guessedNumber(6).betAmount(new BigDecimal("42")).build()
            );

        Game returnedGame = gameRepository.getGame(gameId);
        Player expectedResult = Player.builder().guessedNumber(6).betAmount(new BigDecimal("42")).build();

        assertThat(returnedGame.getPlayers()).hasSize(1);
        assertThat(returnedGame.getPlayers().get("Dave")).usingRecursiveComparison().isEqualTo(expectedResult);
    }

    @Test
    void addPlayer_ifMethodCalledMultipleTimesAtOnce_ensurePersonPutCalledOnce() throws InterruptedException, ExecutionException {
        Game game = builder().gamePhase(GamePhase.BETTING_PHASE)
            .betEndingTime(Instant.now().plus(23, ChronoUnit.MINUTES))
            .winningNumber(24)
            .players(playerMock)
            .build();

        String gameId = gameRepository.createGame(game);

        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Callable<Void>> tasks = new ArrayList<>();

        Integer concurrentRequestCount = 10;
        generatePlayerDataForConcurrentInsert(tasks, gameId, concurrentRequestCount);
        List<Future<Void>> results = executorService.invokeAll(tasks);
        List<Exception> exceptions = new ArrayList<>();

        for (Future<Void> result : results) {
            try {
                result.get();
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        verify(playerMock).put(any(), any());
        assertThat(exceptions).hasSize(concurrentRequestCount - 1);
        assertThat(exceptions).extracting(x -> x.getCause().getMessage()).containsOnly("Player Dave already exists in game");
    }

    @Test
    void getGame_returnsGame_ifExists() {
        Instant bettingEndTime = Instant.now().plus(50, ChronoUnit.SECONDS);

        Game game1 = builder().gamePhase(GamePhase.BETTING_PHASE)
            .betEndingTime(bettingEndTime)
            .players(Map.of("Dave", Player.builder().betAmount(new BigDecimal("5.22")).guessedNumber(4).build()))
            .winningNumber(34)
            .build();

        String gameId = gameRepository.createGame(game1);


        Game expectedObject = builder().gamePhase(GamePhase.BETTING_PHASE)
            .betEndingTime(bettingEndTime)
            .players(Map.of("Dave", Player.builder().betAmount(new BigDecimal("5.22")).guessedNumber(4).build()))
            .winningNumber(34)
            .build();

        Game returnedGame = gameRepository.getGame(gameId);

        assertThat(returnedGame).usingRecursiveComparison().isEqualTo(expectedObject);
    }

    @Test
    void getGame_throwsException_ifGameDoesntExist() {
        assertThatThrownBy(() -> gameRepository.getGame("nonexistingId"))
            .isInstanceOf(GameMissingException.class)
            .hasMessage("Game with the id \"nonexistingId\" does not exist");
    }

    @Test
    void changeGamePhase_ifGameIdProvided_phaseOnlyChangesForSpecifiedGame() {
        Game game1 = builder().gamePhase(GamePhase.BETTING_PHASE)
            .betEndingTime(Instant.now().plus(50, ChronoUnit.SECONDS))
            .winningNumber(34)
            .build();

        Game game2 = builder().gamePhase(GamePhase.RESULTS_PHASE)
            .betEndingTime(Instant.now().plus(50, ChronoUnit.SECONDS))
            .winningNumber(34)
            .build();

        String gameId1 = gameRepository.createGame(game1);
        String gameId2 = gameRepository.createGame(game2);

        gameRepository.changeGamePhase(gameId1, GamePhase.ENDED);

        Game changedGame = gameRepository.getGame(gameId1);
        Game notChangedGame = gameRepository.getGame(gameId2);

        assertThat(changedGame.getGamePhase()).isEqualTo(GamePhase.ENDED);
        assertThat(notChangedGame.getGamePhase()).isEqualTo(GamePhase.RESULTS_PHASE);
    }

    private void generatePlayerDataForConcurrentInsert(List<Callable<Void>> tasks, String gameId, Integer concurrentRequestCount) {
        for(Integer i = 0; i < concurrentRequestCount; i++) {
            tasks.add(() -> {
                gameRepository.addPlayer(
                    gameId,
                    "Dave",
                    Player.builder().guessedNumber(4).betAmount(new BigDecimal(4)).build()
                );
                return null;
            });
        }
    }
}