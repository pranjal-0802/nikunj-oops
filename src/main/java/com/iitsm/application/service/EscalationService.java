package com.iitsm.application.service;

import com.iitsm.domain.chain.escalation.EscalationAction;
import com.iitsm.domain.chain.escalation.EscalationChainBuilder;
import com.iitsm.domain.chain.escalation.EscalationContext;
import com.iitsm.domain.chain.escalation.EscalationHandler;
import com.iitsm.domain.event.DomainEvent;
import com.iitsm.domain.event.DomainEventPublisher;
import com.iitsm.domain.model.Administrator;
import com.iitsm.domain.model.BusinessCriticality;
import com.iitsm.domain.model.EscalationRecord;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.SupportTeam;
import com.iitsm.domain.model.User;
import com.iitsm.domain.model.UserRole;
import com.iitsm.domain.repository.EscalationRecordRepository;
import com.iitsm.domain.repository.IncidentRepository;
import com.iitsm.domain.repository.ItServiceRepository;
import com.iitsm.domain.repository.SupportTeamRepository;
import com.iitsm.domain.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service orchestrating the Multi-Tier Escalation Engine via the Chain of Responsibility.
 */
@Service
@Transactional
public class EscalationService {

    private static final Logger log = LoggerFactory.getLogger(EscalationService.class);

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final SupportTeamRepository supportTeamRepository;
    private final ItServiceRepository itServiceRepository;
    private final EscalationRecordRepository escalationRepository;
    private final DomainEventPublisher eventPublisher;
    private final EscalationHandler escalationChain;
    private final Clock clock;

    public EscalationService(
            IncidentRepository incidentRepository,
            UserRepository userRepository,
            SupportTeamRepository supportTeamRepository,
            ItServiceRepository itServiceRepository,
            EscalationRecordRepository escalationRepository,
            DomainEventPublisher eventPublisher,
            Clock clock) {
        this.incidentRepository = Objects.requireNonNull(incidentRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.supportTeamRepository = Objects.requireNonNull(supportTeamRepository);
        this.itServiceRepository = Objects.requireNonNull(itServiceRepository);
        this.escalationRepository = Objects.requireNonNull(escalationRepository);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.clock = Objects.requireNonNull(clock);
        this.escalationChain = EscalationChainBuilder.buildDefaultChain();
    }

    /**
     * Evaluates an incident against the Multi-Tier Escalation Chain.
     * If an escalation condition is met, creates an EscalationRecord, publishes DomainEvent, and returns record.
     */
    public Optional<EscalationRecord> evaluateAndEscalate(String incidentId) {
        Objects.requireNonNull(incidentId, "incidentId cannot be null");
        Optional<Incident> incidentOpt = incidentRepository.findById(incidentId);
        if (incidentOpt.isEmpty()) {
            return Optional.empty();
        }

        Incident incident = incidentOpt.get();
        Instant now = clock.instant();

        // 1. Resolve context stakeholders
        String leadId = resolveSupportTeamLead(incident);
        String sysAdminId = resolveSystemAdminId();
        String itManagerId = resolveItManagerId();
        boolean isMissionCritical = resolveIsMissionCritical(incident);

        EscalationContext context = new EscalationContext(
                now,
                leadId,
                sysAdminId,
                itManagerId,
                isMissionCritical
        );

        // 2. Evaluate Chain of Responsibility
        Optional<EscalationAction> actionOpt = escalationChain.handle(incident, context);
        if (actionOpt.isEmpty()) {
            return Optional.empty();
        }

        EscalationAction action = actionOpt.get();

        // Check if an identical escalation tier was already triggered recently to prevent spam
        List<EscalationRecord> existing = escalationRepository.findByIncidentIdOrderByEscalatedAtDesc(incidentId);
        if (!existing.isEmpty()) {
            EscalationRecord last = existing.get(0);
            if (last.toRole().equals(action.targetRole()) && last.toUserId().equals(action.targetUserId())) {
                log.debug("Incident {} already escalated to {} ({}) recently; skipping duplicate escalation",
                        incidentId, action.targetUserId(), action.targetRole());
                return Optional.empty();
            }
        }

        // 3. Persist escalation record
        String recordId = "ESC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        EscalationRecord domainRecord = new EscalationRecord(
                recordId,
                incident.getId(),
                "SYSTEM_MONITOR",
                action.targetRole(),
                action.targetUserId(),
                action.reason(),
                now
        );
        escalationRepository.save(domainRecord);

        // 4. Publish DomainEvent.IncidentEscalated
        DomainEvent.IncidentEscalated event = new DomainEvent.IncidentEscalated(
                UUID.randomUUID().toString(),
                now,
                incident.getId(),
                "SYSTEM_MONITOR",
                "SYSTEM_MONITOR",
                action.targetRole(),
                action.targetUserId(),
                action.reason()
        );
        eventPublisher.publish(event);

        log.warn("Escalated incident {} to Tier {} {} (User: {}) - Reason: {}",
                incident.getId(), action.tier(), action.tierName(), action.targetUserId(), action.reason());

        return Optional.of(domainRecord);
    }

    @Transactional(readOnly = true)
    public List<EscalationRecord> getEscalationHistory(String incidentId) {
        return escalationRepository.findByIncidentIdOrderByEscalatedAtDesc(incidentId);
    }

    @Transactional(readOnly = true)
    public List<EscalationRecord> getAllEscalations() {
        return escalationRepository.findAllByOrderByEscalatedAtDesc();
    }

    private String resolveSupportTeamLead(Incident incident) {
        if (incident.getCategory() == null) {
            return null;
        }
        List<SupportTeam> teams = supportTeamRepository.findByHandledCategory(incident.getCategory());
        return teams.isEmpty() ? null : teams.get(0).getTeamLeadId();
    }

    private String resolveSystemAdminId() {
        List<User> admins = userRepository.findByRole(UserRole.ROLE_ADMIN);
        for (User u : admins) {
            if (u instanceof Administrator admin && admin.isSystemAdmin()) {
                return admin.getId();
            }
        }
        return admins.isEmpty() ? "ADMIN-SYS-01" : admins.get(0).getId();
    }

    private String resolveItManagerId() {
        List<User> admins = userRepository.findByRole(UserRole.ROLE_ADMIN);
        for (User u : admins) {
            if (u instanceof Administrator admin && admin.isItManager()) {
                return admin.getId();
            }
        }
        return admins.isEmpty() ? "ADMIN-MGR-01" : admins.get(0).getId();
    }

    private boolean resolveIsMissionCritical(Incident incident) {
        if (incident.getServiceId() == null) {
            return false;
        }
        Optional<ItService> serviceOpt = itServiceRepository.findById(incident.getServiceId());
        return serviceOpt.map(s -> s.getBusinessCriticality() == BusinessCriticality.TIER_4_MISSION_CRITICAL)
                .orElse(false);
    }
}
