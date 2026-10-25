package com.iitsm;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application entrypoint for the Intelligent IT Service Management System (I-ITSM).
 */
@SpringBootApplication
@EnableScheduling
public class IitsmApplication {

    public static void main(String[] args) {
        SpringApplication.run(IitsmApplication.class, args);
    }

    /**
     * Injects standard system UTC clock for production and development profiles.
     * Replaced by AdjustableClock in simulation and testing.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

