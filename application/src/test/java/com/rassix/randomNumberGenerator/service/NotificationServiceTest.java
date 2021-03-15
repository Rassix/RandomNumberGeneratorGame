package com.rassix.randomNumberGenerator.service;

import com.rassix.randomNumberGenerator.constant.GamePhase;
import com.rassix.randomNumberGenerator.controller.dto.GameInfoResponse;
import com.rassix.randomNumberGenerator.service.dto.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void notifyBettingEnd_ifCalled_sentWithEmptyBody() {
        notificationService.notifyBettingEnd();

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        Notification expectedObject = Notification.builder().messageCode("BIDDING_CLOSED").build();

        verify(simpMessagingTemplate).convertAndSend(eq("/topic/biddingClosed"), notificationCaptor.capture());

        assertThat(notificationCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedObject);
    }

    @Test
    void notifyNewGameStart_ifCalled_sendsDataAboutNewGame() {
        String gameId = "idOfGame";
        Instant bettingEndTime = Instant.now().plus(20, ChronoUnit.SECONDS);

        notificationService.notifyNewGameStart(gameId, bettingEndTime);

        ArgumentCaptor<GameInfoResponse> gameInfoCaptor = ArgumentCaptor.forClass(GameInfoResponse.class);

        verify(simpMessagingTemplate).convertAndSend(eq("/topic/newGame"), gameInfoCaptor.capture());

        GameInfoResponse expectedObject = GameInfoResponse.builder()
            .gameId(gameId)
            .gamePhase(GamePhase.BETTING_PHASE)
            .bettingEndTime(bettingEndTime)
            .build();

        assertThat(gameInfoCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedObject);
    }

    @Test
    void sendMessageToPlayer_ifCalled_messageIsSentToSpecifiedUser() {
        String username = "Dave";
        Notification notification = Notification.builder().messageCode("CASUAL_MESSAGE").messagePayload("Have a good day!").build();

        notificationService.sendMessageToPlayer(username, notification);

        verify(simpMessagingTemplate).convertAndSend("/topic/messages/Dave", notification);
    }

    @Test
    void sendGlobalMessage_ifCalled_messageIsSentToEveryoneWhoIsSubscribed() {
        Notification notification = Notification.builder().messageCode("CASUAL_MESSAGE").messagePayload("Have a good day!").build();
        notificationService.sendGlobalMessage(notification);

        verify(simpMessagingTemplate).convertAndSend("/topic/messages/all", notification);
    }
}
