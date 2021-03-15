package com.rassix.randomNumberGenerator.controller;

import com.rassix.randomNumberGenerator.constant.GamePhase;
import com.rassix.randomNumberGenerator.controller.dto.GameInfoResponse;
import com.rassix.randomNumberGenerator.repository.model.Game;
import com.rassix.randomNumberGenerator.service.GameService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

import static java.util.Objects.isNull;

@Slf4j
@Controller
@AllArgsConstructor
public class GameController {

    private final GameService gameService;

    @SubscribeMapping("/topic/getActiveGame")
    public GameInfoResponse gameDataEventSubscription() {
        Map.Entry<String, Game> activeGame = gameService.getActiveGame();

        return !isNull(activeGame) ? GameInfoResponse.builder()
                .gameId(activeGame.getKey())
                .gamePhase(activeGame.getValue().getGamePhase())
                .bettingEndTime(activeGame.getValue().getBetEndingTime())
                .build()
            : GameInfoResponse.builder().gamePhase(GamePhase.NO_ACTIVE_GAME).build();
    }

}
