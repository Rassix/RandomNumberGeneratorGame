package com.rassix.randomNumberGenerator.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WinningPlayer {

    private String name;
    private BigDecimal wonAmount;

}
