package com.rassix.randomNumberGenerator.controller.dto;

import com.rassix.randomNumberGenerator.constant.GamePhase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameInfoResponse {

    private String gameId;
    private GamePhase gamePhase;
    private Instant bettingEndTime;

}
