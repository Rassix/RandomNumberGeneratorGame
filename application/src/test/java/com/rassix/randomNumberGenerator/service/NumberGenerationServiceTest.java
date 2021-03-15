package com.rassix.randomNumberGenerator.service;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class NumberGenerationServiceTest {

    private NumberGenerationService numberGenerationService = new NumberGenerationService(new Random());

    @Test
    void generateNumber_ifCalled_isNotBelow1AndAbove10() {
        for(int i = 0; i < 200; i++) {
            Integer randomNumber = numberGenerationService.generateNumber();
            assertThat(randomNumber).isBetween(1, 10);
        }
    }

}