package com.iitsm.application.service;

import com.iitsm.domain.event.DomainEvent;
import com.iitsm.domain.event.DomainEventPublisher;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.repository.IncidentRepository;
import com.iitsm.domain.strategy.similarity.SimilarityMeasure;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OOP: Application service identifying duplicate tickets and correlating recurring incidents.
 */
@Service
public class DuplicateDetectionService {

    public static final double DUPLICATE_THRESHOLD = 0.70;
    private static final Duration RECENCY_WINDOW = Duration.ofHours(72);

    public record DuplicateMatch(String parentIncidentId, double similarityScore) {}

    private final IncidentRepository incidentRepository;
    private final DomainEventPublisher eventPublisher;
    private final SimilarityMeasure similarityMeasure;
    private final Clock clock;

    public DuplicateDetectionService(
            IncidentRepository incidentRepository,
            DomainEventPublisher eventPublisher,
            SimilarityMeasure similarityMeasure,
            Clock clock) {
        this.incidentRepository = Objects.requireNonNull(incidentRepository, "incidentRepository cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher cannot be null");
        this.similarityMeasure = Objects.requireNonNull(similarityMeasure, "similarityMeasure cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    /**
     * Checks if a new incident is a duplicate of a recent active incident in the same or related domain.
     * If similarity >= 0.70, links the incidents bidirectionally and publishes a DuplicateDetected event.
     */
    @Transactional
    public Optional<DuplicateMatch> checkAndLinkDuplicate(Incident newIncident) {
        Objects.requireNonNull(newIncident, "newIncident cannot be null");

        String newText = (newIncident.getTitle() != null ? newIncident.getTitle() : "") + " "
                + (newIncident.getDescription() != null ? newIncident.getDescription() : "");

        Instant cutoff = clock.instant().minus(RECENCY_WINDOW);
        List<Incident> candidateIncidents;

        if (newIncident.getServiceId() != null && newIncident.getCategory() != null) {
            candidateIncidents = incidentRepository.findOpenIncidentsByServiceAndCategorySince(
                    newIncident.getServiceId(), newIncident.getCategory(), cutoff);
            if (candidateIncidents.isEmpty()) {
                candidateIncidents = incidentRepository.findByCategory(newIncident.getCategory());
            }
        } else if (newIncident.getCategory() != null) {
            candidateIncidents = incidentRepository.findByCategory(newIncident.getCategory());
        } else {
            candidateIncidents = incidentRepository.findOpenIncidentsSince(cutoff);
        }

        Incident bestParent = null;
        double maxSimilarity = 0.0;

        for (Incident candidate : candidateIncidents) {
            // Do not correlate with itself, or closed tickets outside recency window
            if (candidate.getId().equals(newIncident.getId()) || candidate.getStatus() == IncidentStatus.CLOSED) {
                continue;
            }
            if (candidate.getCreatedAt().isBefore(cutoff)) {
                continue;
            }

            String candidateText = (candidate.getTitle() != null ? candidate.getTitle() : "") + " "
                    + (candidate.getDescription() != null ? candidate.getDescription() : "");

            double similarity = similarityMeasure.calculateSimilarity(newText, candidateText);
            if (similarity >= DUPLICATE_THRESHOLD && similarity > maxSimilarity) {
                maxSimilarity = similarity;
                bestParent = candidate;
            }
        }

        if (bestParent != null) {
            Instant now = clock.instant();
            Incident rootParent = findRootParent(bestParent);

            // Bidirectional correlation link to the root parent of the cluster
            rootParent.linkIncident(newIncident.getId());
            newIncident.linkIncident(rootParent.getId());
            incidentRepository.save(rootParent);

            // Also link to bestParent if different from root parent
            if (!bestParent.getId().equals(rootParent.getId())) {
                bestParent.linkIncident(newIncident.getId());
                newIncident.linkIncident(bestParent.getId());
                incidentRepository.save(bestParent);
            }

            // Publish DuplicateDetected domain event
            eventPublisher.publish(new DomainEvent.DuplicateDetected(
                    UUID.randomUUID().toString(),
                    now,
                    newIncident.getId(),
                    "SYSTEM",
                    rootParent.getId(),
                    maxSimilarity
            ));

            return Optional.of(new DuplicateMatch(rootParent.getId(), maxSimilarity));
        }

        return Optional.empty();
    }

    private Incident findRootParent(Incident candidate) {
        Set<String> visited = new HashSet<>();
        return findRootParent(candidate, visited);
    }

    private Incident findRootParent(Incident current, Set<String> visited) {
        visited.add(current.getId());
        Incident oldest = current;
        for (String linkedId : current.getLinkedIncidentIds()) {
            if (!visited.contains(linkedId)) {
                Optional<Incident> opt = incidentRepository.findById(linkedId);
                if (opt.isPresent()) {
                    Incident candidate = opt.get();
                    if (candidate.getCreatedAt().isBefore(oldest.getCreatedAt())) {
                        Incident deepOldest = findRootParent(candidate, visited);
                        if (deepOldest.getCreatedAt().isBefore(oldest.getCreatedAt())) {
                            oldest = deepOldest;
                        }
                    }
                }
            }
        }
        return oldest;
    }

    /**
     * Retrieves all linked duplicate incidents for a given incident.
     */
    @Transactional(readOnly = true)
    public List<Incident> findLinkedDuplicates(String incidentId) {
        if (incidentId == null) {
            return Collections.emptyList();
        }
        Optional<Incident> opt = incidentRepository.findById(incidentId);
        if (opt.isEmpty() || opt.get().getLinkedIncidentIds().isEmpty()) {
            return Collections.emptyList();
        }

        List<Incident> linked = new ArrayList<>();
        for (String linkedId : opt.get().getLinkedIncidentIds()) {
            incidentRepository.findById(linkedId).ifPresent(linked::add);
        }
        return Collections.unmodifiableList(linked);
    }
}

