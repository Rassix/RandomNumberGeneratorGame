package com.rassix.randomNumberGenerator;

import com.rassix.randomNumberGenerator.service.NumberGenerationService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public NumberGenerationService numberGenerationService() {
        NumberGenerationService mock = mock(NumberGenerationService.class);
        when(mock.generateNumber()).thenReturn(3);
        return mock;
    }
}
