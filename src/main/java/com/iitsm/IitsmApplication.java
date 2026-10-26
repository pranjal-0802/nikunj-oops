package com.iitsm;

import com.iitsm.infrastructure.config.AdjustableClock;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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
     * Injects an AdjustableClock instance allowing live simulation acceleration (x1, x60, x600)
     * and fast-forward jumps across the application.
     */
    @Bean
    public Clock clock() {
        return new AdjustableClock(Instant.now(), ZoneId.of("UTC"));
    }
}

