package com.codeguardian.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
    @Bean ObjectMapper objectMapper() { return new ObjectMapper().findAndRegisterModules(); }
}
