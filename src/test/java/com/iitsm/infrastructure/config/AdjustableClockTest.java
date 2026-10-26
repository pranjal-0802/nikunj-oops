package com.iitsm.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AdjustableClock (speed acceleration and fast-forward jumps).
 */
class AdjustableClockTest {

    private Instant baseTime;
    private AdjustableClock clock;

    @BeforeEach
    void setUp() {
        baseTime = Instant.parse("2026-10-26T12:00:00Z");
        clock = new AdjustableClock(baseTime, ZoneId.of("UTC"));
    }

    @Test
    @DisplayName("Initializes with base instant and x1 speed")
    void testInitialState() {
        assertThat(clock.instant()).isAfterOrEqualTo(baseTime);
        assertThat(clock.getSpeedMultiplier()).isEqualTo(1L);
        assertThat(clock.getZone()).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    @DisplayName("Fast-forward immediately advances time by specified duration")
    void testFastForward() {
        clock.fastForward(Duration.ofHours(3));
        assertThat(clock.instant()).isAfterOrEqualTo(baseTime.plus(Duration.ofHours(3)));

        clock.fastForward(Duration.ofDays(1));
        assertThat(clock.instant()).isAfterOrEqualTo(baseTime.plus(Duration.ofHours(27)));
    }

    @Test
    @DisplayName("Accelerates time by specified speed multiplier")
    void testSpeedMultiplier() throws InterruptedException {
        clock.setSpeedMultiplier(600L); // 10 minutes per real second
        assertThat(clock.getSpeedMultiplier()).isEqualTo(600L);

        Instant t0 = clock.instant();
        Thread.sleep(50); // 50ms real time = ~30,000ms (30 seconds) virtual time
        Instant t1 = clock.instant();

        Duration virtualElapsed = Duration.between(t0, t1);
        assertThat(virtualElapsed.toMillis()).isGreaterThan(5000); // Definitely > 5 seconds
    }

    @Test
    @DisplayName("Throws exception on non-positive multiplier")
    void testInvalidMultiplier() {
        assertThatThrownBy(() -> clock.setSpeedMultiplier(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> clock.setSpeedMultiplier(-10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Resets clock to real-world time at x1 speed")
    void testResetClock() {
        clock.fastForward(Duration.ofDays(100));
        clock.setSpeedMultiplier(60L);

        clock.resetToRealTime();
        assertThat(clock.getSpeedMultiplier()).isEqualTo(1L);
        Duration diffFromReal = Duration.between(clock.instant(), Instant.now()).abs();
        assertThat(diffFromReal.toSeconds()).isLessThan(5);
    }
}

