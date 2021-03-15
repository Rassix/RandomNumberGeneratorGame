package com.rassix.randomNumberGenerator.service;

import com.rassix.randomNumberGenerator.constant.GamePhase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameSchedulerTest {

    @Mock
    private NumberGenerationService numberGenerationService;

    @Mock
    private GameService gameService;

    @InjectMocks
    private GameScheduler gameScheduler;

    @Test
    void initGame_ifRan_runsThroughAllGamePhases() throws InterruptedException, ExecutionException {
        Instant now = Instant.now();
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);

        when(numberGenerationService.generateNumber()).thenReturn(3);
        when(gameService.createGame(instantCaptor.capture(), eq(3))).thenReturn("init-game-id");

        gameScheduler.initGame();

        Instant capturedInstant = instantCaptor.getValue();
        assertThat(capturedInstant).isAfter(now);

        verify(gameService).changeGamePhase("init-game-id", GamePhase.RESULTS_PHASE);
        verify(gameService).getUnluckyPlayers("init-game-id");
        verify(gameService).getWinningPlayers("init-game-id");
        verify(gameService).changeGamePhase("init-game-id", GamePhase.ENDED);
    }

}