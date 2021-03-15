package com.rassix.randomNumberGenerator.controller;

import com.rassix.randomNumberGenerator.controller.dto.AddPlayerRequest;
import com.rassix.randomNumberGenerator.controller.dto.ErrorResponse;
import com.rassix.randomNumberGenerator.exception.PlayerExistsException;
import com.rassix.randomNumberGenerator.repository.model.Game;
import com.rassix.randomNumberGenerator.service.GameService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Instant;

@RestController
@AllArgsConstructor
@Slf4j
@Validated
public class PlayerController {

    private final GameService gameService;

    @PostMapping("/game/addPlayer/{gameId}")
    @ResponseBody
    public ResponseEntity<?> addPlayer(@PathVariable("gameId") String gameId, @RequestBody @Valid AddPlayerRequest addPlayerRequest) throws PlayerExistsException {
        Instant requestReceived = Instant.now();
        Game game = gameService.getGameDetails(gameId);

        if (requestReceived.isAfter(game.getBetEndingTime())) {
            log.warn("Player request made after bidding time is over. Aborting operation");
            return new ResponseEntity<>(
                new ErrorResponse("INVALID_BID", "Bidding made after expiry time"),
                HttpStatus.BAD_REQUEST
            );
        }

        gameService.addPlayer(gameId, addPlayerRequest);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

}
