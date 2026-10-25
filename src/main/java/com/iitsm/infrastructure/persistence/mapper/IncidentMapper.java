package com.iitsm.infrastructure.persistence.mapper;

import com.iitsm.domain.model.Feedback;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.InvestigationNote;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.Resolution;
import com.iitsm.domain.model.SlaTracker;
import com.iitsm.infrastructure.persistence.entity.FeedbackJpaEntity;
import com.iitsm.infrastructure.persistence.entity.IncidentJpaEntity;
import com.iitsm.infrastructure.persistence.entity.InvestigationNoteJpaEntity;
import com.iitsm.infrastructure.persistence.entity.ResolutionJpaEntity;
import com.iitsm.infrastructure.persistence.entity.SlaTrackerJpaEntity;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps between pure domain Incident models and IncidentJpaEntity persistence models.
 */
public final class IncidentMapper {

    private IncidentMapper() {}

    public static Incident toDomain(IncidentJpaEntity entity) {
        if (entity == null) return null;

        Incident incident = new Incident(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getReporterId(),
                entity.getCreatedAt());

        incident.transitionTo(entity.getStatus());
        if (entity.getAssigneeId() != null) {
            incident.setAssignee(entity.getAssigneeId());
        }
        incident.setReopenCount(entity.getReopenCount());
        incident.setVersion(entity.getVersion());

        // Classification & Priority
        if (entity.getCategory() != null && entity.getPriority() != null) {
            List<String> rules = (entity.getRulesApplied() != null && !entity.getRulesApplied().isEmpty())
                    ? Arrays.asList(entity.getRulesApplied().split(","))
                    : Collections.emptyList();
            PriorityDecision decision = new PriorityDecision(
                    entity.getPriority(),
                    entity.getCompositeScore() != null ? entity.getCompositeScore() : 0.0,
                    Collections.emptyList(),
                    rules,
                    entity.getPriorityCalculatedAt() != null ? entity.getPriorityCalculatedAt() : entity.getCreatedAt(),
                    entity.getPriorityOverriddenBy(),
                    entity.getPriorityOverrideReason());
            incident.setClassificationData(entity.getCategory(), entity.getServiceId(), decision);
        }

        // SLA Tracker
        if (entity.getSlaTracker() != null) {
            SlaTrackerJpaEntity trackerEntity = entity.getSlaTracker();
            SlaTracker tracker = SlaTracker.reconstitute(
                    trackerEntity.getCreatedAt(),
                    trackerEntity.getResponseDueAt(),
                    trackerEntity.getResolutionDueAt(),
                    trackerEntity.getRespondedAt(),
                    trackerEntity.getResolvedAt(),
                    Duration.ofSeconds(trackerEntity.getPausedDurationSeconds()),
                    trackerEntity.getPausedAt(),
                    trackerEntity.getStatus());
            incident.setSlaTracker(tracker);
        }

        // Notes
        if (entity.getNotes() != null) {
            for (InvestigationNoteJpaEntity noteEntity : entity.getNotes()) {
                incident.addNote(new InvestigationNote(
                        noteEntity.getId(),
                        noteEntity.getAuthorId(),
                        noteEntity.getAuthorName(),
                        noteEntity.getNoteText(),
                        noteEntity.getCreatedAt(),
                        noteEntity.isInternalOnly()));
            }
        }

        // Linked Incidents
        if (entity.getLinkedIncidentIds() != null) {
            entity.getLinkedIncidentIds().forEach(incident::linkIncident);
        }

        // Resolution
        if (entity.getResolution() != null) {
            ResolutionJpaEntity res = entity.getResolution();
            incident.setResolution(new Resolution(
                    res.getRootCause(),
                    res.getStepsTaken(),
                    res.getPreventiveAction(),
                    res.getConfirmedCategory(),
                    res.getResolvedBy(),
                    res.getResolvedAt()));
        }

        // Feedback
        if (entity.getFeedback() != null) {
            FeedbackJpaEntity fb = entity.getFeedback();
            incident.setFeedback(new Feedback(fb.getRating(), fb.getComment(), fb.getSubmittedAt()));
        }

        return incident;
    }

    public static IncidentJpaEntity toJpaEntity(Incident domain) {
        if (domain == null) return null;

        IncidentJpaEntity entity = new IncidentJpaEntity();
        entity.setId(domain.getId());
        entity.setTitle(domain.getTitle());
        entity.setDescription(domain.getDescription());
        entity.setReporterId(domain.getReporterId());
        entity.setCategory(domain.getCategory());
        entity.setServiceId(domain.getServiceId());
        entity.setStatus(domain.getStatus());
        entity.setAssigneeId(domain.getAssigneeId());
        entity.setReopenCount(domain.getReopenCount());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setVersion(domain.getVersion());
        entity.setLinkedIncidentIds(new HashSet<>(domain.getLinkedIncidentIds()));

        if (domain.getPriorityDecision() != null) {
            PriorityDecision pd = domain.getPriorityDecision();
            entity.setPriority(pd.priority());
            entity.setCompositeScore(pd.compositeScore());
            entity.setRulesApplied(String.join(",", pd.rulesApplied()));
            entity.setPriorityCalculatedAt(pd.calculatedAt());
            entity.setPriorityOverriddenBy(pd.overriddenBy());
            entity.setPriorityOverrideReason(pd.overrideReason());
        }

        if (domain.getSlaTracker() != null) {
            SlaTracker tracker = domain.getSlaTracker();
            SlaTrackerJpaEntity trackerEntity = new SlaTrackerJpaEntity();
            trackerEntity.setIncidentId(domain.getId());
            trackerEntity.setCreatedAt(tracker.getCreatedAt());
            trackerEntity.setResponseDueAt(tracker.getResponseDueAt());
            trackerEntity.setResolutionDueAt(tracker.getResolutionDueAt());
            trackerEntity.setRespondedAt(tracker.getRespondedAt());
            trackerEntity.setResolvedAt(tracker.getResolvedAt());
            trackerEntity.setPausedDurationSeconds(tracker.getPausedDuration().toSeconds());
            trackerEntity.setPausedAt(tracker.getPausedAt());
            trackerEntity.setStatus(tracker.getStatus());
            entity.setSlaTracker(trackerEntity);
        }

        if (domain.getNotes() != null) {
            List<InvestigationNoteJpaEntity> noteEntities = new ArrayList<>();
            for (InvestigationNote note : domain.getNotes()) {
                InvestigationNoteJpaEntity ne = new InvestigationNoteJpaEntity();
                ne.setId(note.id());
                ne.setIncident(entity);
                ne.setAuthorId(note.authorId());
                ne.setAuthorName(note.authorName());
                ne.setNoteText(note.noteText());
                ne.setInternalOnly(note.internalOnly());
                ne.setCreatedAt(note.createdAt());
                noteEntities.add(ne);
            }
            entity.setNotes(noteEntities);
        }

        if (domain.getResolution() != null) {
            Resolution r = domain.getResolution();
            ResolutionJpaEntity re = new ResolutionJpaEntity();
            re.setIncidentId(domain.getId());
            re.setRootCause(r.rootCause());
            re.setStepsTaken(r.stepsTaken());
            re.setPreventiveAction(r.preventiveAction());
            re.setConfirmedCategory(r.confirmedCategory());
            re.setResolvedBy(r.resolvedBy());
            re.setResolvedAt(r.resolvedAt());
            entity.setResolution(re);
        }

        if (domain.getFeedback() != null) {
            Feedback f = domain.getFeedback();
            FeedbackJpaEntity fe = new FeedbackJpaEntity();
            fe.setIncidentId(domain.getId());
            fe.setRating(f.rating());
            fe.setComment(f.comment());
            fe.setSubmittedAt(f.submittedAt());
            entity.setFeedback(fe);
        }

        return entity;
    }
}
