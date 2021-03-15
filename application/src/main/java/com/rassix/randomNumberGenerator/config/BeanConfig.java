package com.rassix.randomNumberGenerator.config;

import com.rassix.randomNumberGenerator.repository.GameRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class BeanConfig {

    @Bean
    public Random random() {
        return new Random();
    }

    @Bean
    public GameRepository gameRepository() {
        return new GameRepository();
    }
}
