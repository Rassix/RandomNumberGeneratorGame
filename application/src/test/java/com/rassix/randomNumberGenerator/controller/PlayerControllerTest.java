package com.rassix.randomNumberGenerator.controller;

import com.rassix.randomNumberGenerator.controller.dto.ErrorResponse;
import com.rassix.randomNumberGenerator.exception.PlayerExistsException;
import com.rassix.randomNumberGenerator.repository.model.Game;
import com.rassix.randomNumberGenerator.service.GameService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerControllerTest {

    @Mock
    private GameService gameService;

    @InjectMocks
    private PlayerController playerController;

    @Test
    void addPlayer_ifRequestMadeAfterBiddingEnded_returnsStatus400() throws PlayerExistsException {
        String gameId = "testGameId";

        when(gameService.getGameDetails(gameId)).thenReturn(
            Game.builder().betEndingTime(Instant.now().minus(50, ChronoUnit.SECONDS)).build()
        );

        ResponseEntity<?> responseEntity = playerController.addPlayer(gameId, null);

        ErrorResponse expectedErrorBody = new ErrorResponse("INVALID_BID", "Bidding made after expiry time");

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseEntity.getBody()).usingRecursiveComparison().isEqualTo(expectedErrorBody);
    }

}