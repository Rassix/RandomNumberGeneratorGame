package com.rassix.randomNumberGenerator.repository.model;

import com.rassix.randomNumberGenerator.constant.GamePhase;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Game {

    private Instant betEndingTime;
    @Setter
    private GamePhase gamePhase;
    private Integer winningNumber;
    @Builder.Default
    private Map<String, Player> players = new HashMap<>();

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Player {
        private BigDecimal betAmount;
        private Integer guessedNumber;
    }
}
