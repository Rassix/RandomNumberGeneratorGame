package com.rassix.randomNumberGenerator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rassix.randomNumberGenerator.Application;
import com.rassix.randomNumberGenerator.TestConfig;
import com.rassix.randomNumberGenerator.constant.GamePhase;
import com.rassix.randomNumberGenerator.controller.dto.AddPlayerRequest;
import com.rassix.randomNumberGenerator.controller.dto.GameInfoResponse;
import com.rassix.randomNumberGenerator.service.dto.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
@Import(TestConfig.class)
public class GameSchedulerTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    WebSocketStompClient stompClient;

    @BeforeEach
    public void setup() {
        stompClient = new WebSocketStompClient(new SockJsClient(createTransportClient()));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @Test
    void shouldReceiveInfoAboutOngoingGame_ifSubscribed() throws InterruptedException, ExecutionException, TimeoutException {
        BlockingQueue<GameInfoResponse> blockingQueue = new ArrayBlockingQueue(1);

        StompSession session = stompClient
            .connect(getWsPath(), new StompSessionHandlerAdapter() {})
            .get(1, SECONDS);

        session.subscribe("/app/topic/getActiveGame", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                GameInfoResponse gameInfoResponse;
                try {
                    gameInfoResponse = objectMapper.readValue(new String((byte[]) payload), GameInfoResponse.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException();
                }
                blockingQueue.add(gameInfoResponse);
            }
        });

        GameInfoResponse result = blockingQueue.poll(5, SECONDS);
        assertThat(result.getGameId()).isNotNull();
        assertThat(result.getGamePhase()).isEqualTo(GamePhase.BETTING_PHASE);
        assertThat(result.getBettingEndTime()).isAfter(Instant.now());
    }

    @Test
    void shouldReceiveBiddingClosedMessage_ifBackendSends() throws Exception {
        BlockingQueue<Notification> blockingQueue = new ArrayBlockingQueue(1);

        StompSession session = stompClient
            .connect(getWsPath(), new StompSessionHandlerAdapter() {})
            .get(1, SECONDS);

        session.subscribe("/topic/biddingClosed", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                Notification notification;
                try {
                    notification = objectMapper.readValue(new String((byte[]) payload), Notification.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException();
                }

                blockingQueue.add(notification);
            }
        });

        Notification notification = blockingQueue.poll(10, SECONDS);
        assertThat(notification.getMessagePayload()).isNull();
        assertThat(notification.getMessageCode()).isEqualTo("BIDDING_CLOSED");
    }


    @Test
    public void shouldReceiveNewGameMessage_ifBackendSends() throws Exception {
        BlockingQueue<GameInfoResponse> blockingQueue = new ArrayBlockingQueue(1);

        StompSession session = stompClient
            .connect(getWsPath(), new StompSessionHandlerAdapter() {})
            .get(1, SECONDS);

        session.subscribe("/topic/newGame", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                GameInfoResponse gameInfoResponse;
                try {
                    gameInfoResponse = objectMapper.readValue(new String((byte[]) payload), GameInfoResponse.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException();
                }
                blockingQueue.add(gameInfoResponse);
            }
        });

        GameInfoResponse result = blockingQueue.poll(16, SECONDS);
        assertThat(result.getGameId()).isNotNull();
        assertThat(result.getGamePhase()).isEqualTo(GamePhase.BETTING_PHASE);
        assertThat(result.getBettingEndTime()).isAfter(Instant.now());
    }

    @Test
    void shouldReceiveLossMessage_ifBackendSendsIt() throws Exception {
        BlockingQueue<Notification> blockingQueue = new ArrayBlockingQueue(1);
        BlockingQueue<GameInfoResponse> gameInfoBlockingQueue = new ArrayBlockingQueue(1);

        StompSession session = stompClient
            .connect(getWsPath(), new StompSessionHandlerAdapter() {})
            .get(1, SECONDS);

        session.subscribe("/app/topic/getActiveGame", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                GameInfoResponse gameInfoResponse;
                try {
                    gameInfoResponse = objectMapper.readValue(new String((byte[]) payload), GameInfoResponse.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException();
                }
                gameInfoBlockingQueue.add(gameInfoResponse);
            }
        });

        session.subscribe("/topic/messages/dave", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                Notification notification;
                try {
                    notification = objectMapper.readValue(new String((byte[]) payload), Notification.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException();
                }
                blockingQueue.add(notification);
            }
        });

        GameInfoResponse gameInfo = gameInfoBlockingQueue.poll(3, SECONDS);

        mockMvc.perform( MockMvcRequestBuilders
            .post("/game/addPlayer/" + gameInfo.getGameId())
            .content(asJsonString(new AddPlayerRequest("dave", 2, new BigDecimal("30"))))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        Notification result = blockingQueue.poll(16, SECONDS);
        assertThat(result.getMessageCode()).isEqualTo("LOSS_NOTIFICATION");
        assertThat(result.getMessagePayload()).isNull();
    }

    @Test
    void shouldReceiveWinMessage_ifBackendSendsIt() throws Exception {
        BlockingQueue<String> winNotificationBlockingQueue = new ArrayBlockingQueue(1);
        BlockingQueue<String> globalWinNotificationBlockingQueue = new ArrayBlockingQueue(1);
        BlockingQueue<GameInfoResponse> gameInfoBlockingQueue = new ArrayBlockingQueue(1);

        StompSession session = stompClient
            .connect(getWsPath(), new StompSessionHandlerAdapter() {})
            .get(1, SECONDS);

        session.subscribe("/app/topic/getActiveGame", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                GameInfoResponse gameInfoResponse;
                try {
                    gameInfoResponse = objectMapper.readValue(new String((byte[]) payload), GameInfoResponse.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException();
                }
                gameInfoBlockingQueue.add(gameInfoResponse);
            }
        });

        session.subscribe("/topic/messages/dave", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String notificationJson = new String((byte[]) payload);
                winNotificationBlockingQueue.add(notificationJson);
            }
        });

        session.subscribe("/topic/messages/all", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String notification = new String((byte[]) payload);
                globalWinNotificationBlockingQueue.add(notification);
            }
        });

        GameInfoResponse gameInfo = gameInfoBlockingQueue.poll(3, SECONDS);

        mockMvc.perform( MockMvcRequestBuilders
            .post("/game/addPlayer/" + gameInfo.getGameId())
            .content(asJsonString(new AddPlayerRequest("dave", 3, new BigDecimal("31"))))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated());

        String winNotificationJson = winNotificationBlockingQueue.poll(15, SECONDS);
        String globalWinNotification = globalWinNotificationBlockingQueue.poll(15, SECONDS);

        assertThat(winNotificationJson).isEqualTo(
            "{\"messageCode\":\"WIN_NOTIFICATION\",\"messagePayload\":{\"name\":\"dave\",\"wonAmount\":306.90}}"
        );

        assertThat(globalWinNotification).isEqualTo(
            "{\"messageCode\":\"WINNER_LISTING\",\"messagePayload\":[{\"name\":\"dave\",\"wonAmount\":306.90}]}"
        );
    }

    private String getWsPath() {
        return String.format("ws://localhost:%d/game", port);
    }

    private List<Transport> createTransportClient() {
        List<Transport> transports = new ArrayList<>(1);
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        return transports;
    }

    private String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
