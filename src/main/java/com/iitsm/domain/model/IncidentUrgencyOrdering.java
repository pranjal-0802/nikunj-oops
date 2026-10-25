package com.iitsm.domain.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Comparator;

/**
 * Pattern: Strategy / Comparator.
 * Strict urgency ordering for technician queues.
 * Sorts incidents by priority (P1 > P2 > P3 > P4), then by SLA due time, then by creation timestamp.
 */
public class IncidentUrgencyOrdering implements Comparator<Incident>, Serializable {

    private static final long serialVersionUID = 1L;

    public static final IncidentUrgencyOrdering INSTANCE = new IncidentUrgencyOrdering();

    @Override
    public int compare(Incident o1, Incident o2) {
        if (o1 == o2) return 0;
        if (o1 == null) return 1;
        if (o2 == null) return -1;

        // 1. Priority rank comparison (lower rank integer = higher urgency: P1=1, P4=4)
        int p1Rank = (o1.getPriorityDecision() != null && o1.getPriorityDecision().priority() != null)
                ? o1.getPriorityDecision().priority().getRank()
                : Integer.MAX_VALUE;
        int p2Rank = (o2.getPriorityDecision() != null && o2.getPriorityDecision().priority() != null)
                ? o2.getPriorityDecision().priority().getRank()
                : Integer.MAX_VALUE;

        if (p1Rank != p2Rank) {
            return Integer.compare(p1Rank, p2Rank);
        }

        // 2. SLA due time comparison (earlier due time = higher urgency)
        Instant d1 = (o1.getSlaTracker() != null) ? o1.getSlaTracker().getResolutionDueAt() : null;
        Instant d2 = (o2.getSlaTracker() != null) ? o2.getSlaTracker().getResolutionDueAt() : null;

        if (d1 != null && d2 != null) {
            int dueDiff = d1.compareTo(d2);
            if (dueDiff != 0) return dueDiff;
        } else if (d1 != null) {
            return -1;
        } else if (d2 != null) {
            return 1;
        }

        // 3. FIFO creation time fallback
        return o1.getCreatedAt().compareTo(o2.getCreatedAt());
    }
}

