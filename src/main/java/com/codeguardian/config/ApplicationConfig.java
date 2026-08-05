package com.codeguardian.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class ApplicationConfig {
    @Bean ObjectMapper objectMapper() { return new ObjectMapper().findAndRegisterModules(); }
    @Bean BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
