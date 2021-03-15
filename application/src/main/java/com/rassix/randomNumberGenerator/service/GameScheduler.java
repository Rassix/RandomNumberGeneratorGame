package com.rassix.randomNumberGenerator.service;


import com.rassix.randomNumberGenerator.constant.GamePhase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@AllArgsConstructor
@Service
@Slf4j
public class GameScheduler {

    private final NumberGenerationService numberGenerationService;
    private final GameService gameService;

    @Scheduled(initialDelay = 0, fixedDelay = 2500)
    public void initGame() throws InterruptedException, ExecutionException {
        Instant now = Instant.now();
        Instant betEndingTime = now.plus(10, ChronoUnit.SECONDS);

        log.info("Starting a new game");
        Integer winningNumber = numberGenerationService.generateNumber();
        System.out.println(winningNumber);
        String gameId = gameService.createGame(betEndingTime, winningNumber);
        log.info("Game with id " + gameId + " created");

        Thread.sleep(ChronoUnit.MILLIS.between(now, betEndingTime));

        log.info("Betting phase ended");
        gameService.changeGamePhase(gameId, GamePhase.RESULTS_PHASE);
        broadCastRoundResults(gameId);

        gameService.changeGamePhase(gameId, GamePhase.ENDED);
        log.info("Game with id " + gameId + " ended");
    }

    private void broadCastRoundResults(String gameId) throws ExecutionException, InterruptedException {
        CompletableFuture<Void> notifyWinnersResult = CompletableFuture.supplyAsync(
            () -> gameService.getWinningPlayers(gameId)
        ).thenAccept(gameService::notifyOfWinners);

        CompletableFuture<Void> notifyUnluckyPlayers = CompletableFuture.supplyAsync(
            () -> gameService.getUnluckyPlayers(gameId)
        ).thenAccept(gameService::notifyLoss);

        notifyWinnersResult.get();
        notifyUnluckyPlayers.get();
    }

}
