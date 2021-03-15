package com.rassix.randomNumberGenerator.controller;

import com.rassix.randomNumberGenerator.constant.GamePhase;
import com.rassix.randomNumberGenerator.controller.dto.AddPlayerRequest;
import com.rassix.randomNumberGenerator.controller.dto.ErrorResponse;
import com.rassix.randomNumberGenerator.controller.dto.GameInfoResponse;
import com.rassix.randomNumberGenerator.exception.PlayerExistsException;
import com.rassix.randomNumberGenerator.repository.model.Game;
import com.rassix.randomNumberGenerator.service.GameService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.Map;

import static java.util.Objects.isNull;

@Slf4j
@Controller
@AllArgsConstructor
public class GameController {

    private final GameService gameService;
    private final SimpMessagingTemplate simpMessagingTemplate;

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
