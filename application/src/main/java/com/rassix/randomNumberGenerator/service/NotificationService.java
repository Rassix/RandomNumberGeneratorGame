package com.rassix.randomNumberGenerator.service;

import com.rassix.randomNumberGenerator.constant.GamePhase;
import com.rassix.randomNumberGenerator.controller.dto.GameInfoResponse;
import com.rassix.randomNumberGenerator.service.dto.Notification;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@AllArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public void notifyBettingEnd() {
        log.info("sending message about bidding closed");
        simpMessagingTemplate.convertAndSend("/topic/biddingClosed",
            Notification.builder().messageCode("BIDDING_CLOSED").build()
        );
    }

    public void notifyNewGameStart(String gameId, Instant bettingEndTime) {
        log.info("sending message about new game");

        simpMessagingTemplate.convertAndSend(
                "/topic/newGame",
                GameInfoResponse.builder()
                        .gameId(gameId)
                        .bettingEndTime(bettingEndTime)
                        .gamePhase(GamePhase.BETTING_PHASE)
                        .build()
        );
    }

    public void sendGlobalMessage(Notification notification) {
        simpMessagingTemplate.convertAndSend("/topic/messages/all", notification);
    }

    public void sendMessageToPlayer(String username, Notification notification) {
        simpMessagingTemplate.convertAndSend("/topic/messages/" + username, notification);
    }

}
