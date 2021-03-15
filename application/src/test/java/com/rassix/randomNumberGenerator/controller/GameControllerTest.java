package com.rassix.randomNumberGenerator.controller;

import com.rassix.randomNumberGenerator.constant.GamePhase;
import com.rassix.randomNumberGenerator.controller.dto.GameInfoResponse;
import com.rassix.randomNumberGenerator.repository.model.Game;
import com.rassix.randomNumberGenerator.service.GameService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private GameService gameService;

    @InjectMocks
    private GameController gameController;

    @Test
    void gameDataEventSubscription_ifActiveGameIsFound_activeGameIsReturned() {
        String gameId = "testGameId";
        Instant bettingEndTime = Instant.now().plus(10, ChronoUnit.SECONDS);

        Game game = Game.builder()
            .gamePhase(GamePhase.BETTING_PHASE)
            .betEndingTime(bettingEndTime)
            .winningNumber(5)
            .build();

        when(gameService.getActiveGame()).thenReturn(
            Map.entry(gameId, game)
        );

        GameInfoResponse expectedObject = GameInfoResponse.builder()
            .gameId(gameId)
            .bettingEndTime(bettingEndTime)
            .gamePhase(GamePhase.BETTING_PHASE)
            .build();

        GameInfoResponse gameInfoResponse = gameController.gameDataEventSubscription();

        assertThat(gameInfoResponse).usingRecursiveComparison().isEqualTo(expectedObject);
    }

    @Test
    void gameDataEventSubscription_ifActiveGameNotFound_returnsNull() {
        when(gameService.getActiveGame()).thenReturn(null);

        GameInfoResponse gameInfoResponse = gameController.gameDataEventSubscription();

        GameInfoResponse expectedResponse = GameInfoResponse.builder().gamePhase(GamePhase.NO_ACTIVE_GAME).build();

        assertThat(gameInfoResponse).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

}