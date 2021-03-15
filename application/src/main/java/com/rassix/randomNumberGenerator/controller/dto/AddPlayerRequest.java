package com.rassix.randomNumberGenerator.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddPlayerRequest {

    @NotNull
    private String username;

    @NotNull
    @Min(1)
    @Max(10)
    private Integer guessedNumber;

    @NotNull
    @Positive
    private BigDecimal bid;

}
