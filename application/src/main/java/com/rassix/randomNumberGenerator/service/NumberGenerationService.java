package com.rassix.randomNumberGenerator.service;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class NumberGenerationService {

    private final Random random;

    private final Integer LOW = 1;
    private final Integer HIGH = 10;

    public NumberGenerationService(Random random) {
        this.random = random;
    }

    public Integer generateNumber() {
        return random.nextInt(HIGH - LOW + 1) + LOW;
    }
}
