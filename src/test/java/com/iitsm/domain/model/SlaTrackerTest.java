package com.iitsm.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * OOP: Service Level Agreement deadline and status calculation tests.
 * Validates warning thresholds, breach detection, and pause/resume windows.
 */
class SlaTrackerTest {

    private Instant createdAt;
    private SlaPolicy p1Policy;

    @BeforeEach
    void setUp() {
        createdAt = Instant.parse("2026-10-25T10:00:00Z");
        p1Policy = SlaPolicy.defaultFor(Priority.P1); // 5 min response, 30 min resolution
    }

    @Test
    @DisplayName("SlaTracker properly initialises response and resolution deadlines")
    void testInitialization() {
        SlaTracker tracker = SlaTracker.create(createdAt, p1Policy);

        assertThat(tracker.getCreatedAt()).isEqualTo(createdAt);
        assertThat(tracker.getResponseDueAt()).isEqualTo(createdAt.plus(Duration.ofMinutes(5)));
        assertThat(tracker.getResolutionDueAt()).isEqualTo(createdAt.plus(Duration.ofMinutes(30)));
        assertThat(tracker.getStatus()).isEqualTo(SlaStatus.ON_TRACK);
        assertThat(tracker.isPaused()).isFalse();
    }

    @Test
    @DisplayName("SLA Status transitions: ON_TRACK -> AT_RISK (75%) -> WARNING (90%) -> BREACHED (100%)")
    void testSlaThresholdTransitions() {
        SlaTracker tracker = SlaTracker.create(createdAt, p1Policy);
        tracker.recordResponse(createdAt.plusSeconds(60)); // Responded in 1 min (within 5m)

        // 30 min window total:
        // 70% elapsed = 21 mins -> ON_TRACK
        tracker.evaluateStatus(createdAt.plus(Duration.ofMinutes(21)));
        assertThat(tracker.getStatus()).isEqualTo(SlaStatus.ON_TRACK);

        // 76% elapsed = 23 mins -> AT_RISK (>= 75%)
        tracker.evaluateStatus(createdAt.plus(Duration.ofMinutes(23)));
        assertThat(tracker.getStatus()).isEqualTo(SlaStatus.AT_RISK);

        // 92% elapsed = 28 mins -> WARNING (>= 90%)
        tracker.evaluateStatus(createdAt.plus(Duration.ofMinutes(28)));
        assertThat(tracker.getStatus()).isEqualTo(SlaStatus.WARNING);

        // 101% elapsed = 31 mins -> BREACHED
        tracker.evaluateStatus(createdAt.plus(Duration.ofMinutes(31)));
        assertThat(tracker.getStatus()).isEqualTo(SlaStatus.BREACHED);
    }

    @Test
    @DisplayName("Response deadline breach: unacknowledged ticket after responseTarget is BREACHED")
    void testResponseBreach() {
        SlaTracker tracker = SlaTracker.create(createdAt, p1Policy);
        // At 6 mins (responseTarget is 5 mins), not responded
        tracker.evaluateStatus(createdAt.plus(Duration.ofMinutes(6)));
        assertThat(tracker.getStatus()).isEqualTo(SlaStatus.BREACHED);
    }

    @Test
    @DisplayName("Resolution within SLA results in MET status")
    void testResolvedWithinSla() {
        SlaTracker tracker = SlaTracker.create(createdAt, p1Policy);
        tracker.recordResponse(createdAt.plus(Duration.ofMinutes(2)));
        tracker.recordResolution(createdAt.plus(Duration.ofMinutes(20))); // Within 30 min window

        assertThat(tracker.getStatus()).isEqualTo(SlaStatus.MET);
        assertThat(tracker.getResolvedAt()).isEqualTo(createdAt.plus(Duration.ofMinutes(20)));
    }

    @Test
    @DisplayName("Resolution after SLA deadline results in BREACHED status")
    void testResolvedAfterSlaDeadline() {
        SlaTracker tracker = SlaTracker.create(createdAt, p1Policy);
        tracker.recordResponse(createdAt.plus(Duration.ofMinutes(2)));
        tracker.recordResolution(createdAt.plus(Duration.ofMinutes(35))); // Exceeded 30 min window

        assertThat(tracker.getStatus()).isEqualTo(SlaStatus.BREACHED);
    }

    @Test
    @DisplayName("Clock pauses on PENDING_INFO and extends resolution due deadline by paused duration")
    void testPauseAndResumeClock() {
        SlaTracker tracker = SlaTracker.create(createdAt, p1Policy);
        tracker.recordResponse(createdAt.plus(Duration.ofMinutes(2)));

        Instant pauseStart = createdAt.plus(Duration.ofMinutes(10));
        tracker.pauseClock(pauseStart);

        assertThat(tracker.isPaused()).isTrue();
        assertThat(tracker.getPausedAt()).isEqualTo(pauseStart);

        // Advance 15 minutes while in PENDING_INFO
        Instant resumeTime = pauseStart.plus(Duration.ofMinutes(15));
        tracker.resumeClock(resumeTime);

        assertThat(tracker.isPaused()).isFalse();
        assertThat(tracker.getPausedDuration()).isEqualTo(Duration.ofMinutes(15));

        // Original deadline was +30m (10:30). With 15m pause, new deadline must be 10:45!
        Instant expectedDeadline = createdAt.plus(Duration.ofMinutes(30)).plus(Duration.ofMinutes(15));
        assertThat(tracker.getResolutionDueAt()).isEqualTo(expectedDeadline);

        // At 10:35 (which would have been breached without pause), it is still ON_TRACK!
        tracker.evaluateStatus(createdAt.plus(Duration.ofMinutes(35)));
        assertThat(tracker.getStatus()).isNotEqualTo(SlaStatus.BREACHED);
    }
}
