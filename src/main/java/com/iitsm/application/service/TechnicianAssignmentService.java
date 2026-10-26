package com.iitsm.application.service;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.repository.TechnicianRepository;
import com.iitsm.domain.strategy.assignment.AssignmentRecommendation;
import com.iitsm.domain.strategy.assignment.AssignmentStrategy;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing technician assignment within an isolated transaction boundary (REQUIRES_NEW).
 * This ensures that concurrent capacity contention (optimistic locking conflicts on technicians)
 * does not mark the caller's outer incident creation transaction as rollback-only.
 */
@Service
public class TechnicianAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(TechnicianAssignmentService.class);

    private final TechnicianRepository technicianRepository;

    public TechnicianAssignmentService(TechnicianRepository technicianRepository) {
        this.technicianRepository = Objects.requireNonNull(technicianRepository, "technicianRepository cannot be null");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<String> tryAssignTechnician(Incident incident, List<Technician> candidates, AssignmentStrategy strategy, Instant now) {
        if (candidates == null || candidates.isEmpty() || strategy == null) {
            return Optional.empty();
        }

        try {
            AssignmentRecommendation recommendation = strategy.recommend(incident, candidates);
            Technician recommended = recommendation.technician();
            if (recommended == null) {
                return Optional.empty();
            }

            // Reload fresh technician inside this isolated transaction to verify real-time capacity and current version
            Optional<Technician> optTech = technicianRepository.findById(recommended.getId());
            if (optTech.isEmpty()) {
                return Optional.empty();
            }

            Technician freshTech = optTech.get();
            if (!freshTech.hasCapacity()) {
                log.info("Technician {} has reached maximum capacity", freshTech.getId());
                return Optional.empty();
            }

            freshTech.assignIncident(incident.getId());
            technicianRepository.save(freshTech);
            return Optional.of(freshTech.getId());
        } catch (Exception ex) {
            log.warn("Technician assignment contention for incident {}: {}", incident.getId(), ex.getMessage());
            return Optional.empty();
        }
    }
}

