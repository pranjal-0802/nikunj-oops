package com.iitsm.infrastructure.persistence.entity;

import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.SlaStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JPA entity representing Incident aggregate root persistence.
 * Includes @Version for optimistic locking to safeguard against concurrent assignment race conditions.
 */
@Entity
@Table(name = "incidents")
public class IncidentJpaEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "reporter_id", nullable = false, length = 64)
    private String reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 32)
    private IncidentCategory category;

    @Column(name = "service_id", length = 64)
    private String serviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 16)
    private Priority priority;

    @Column(name = "composite_score")
    private Double compositeScore;

    @Column(name = "rules_applied", columnDefinition = "TEXT")
    private String rulesApplied;

    @Column(name = "priority_calculated_at")
    private Instant priorityCalculatedAt;

    @Column(name = "priority_overridden_by", length = 64)
    private String priorityOverriddenBy;

    @Column(name = "priority_override_reason", columnDefinition = "TEXT")
    private String priorityOverrideReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private IncidentStatus status;

    @Column(name = "assignee_id", length = 64)
    private String assigneeId;

    @Column(name = "reopen_count", nullable = false)
    private int reopenCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version")
    private Long version = 0L;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "incident_linked_ids", joinColumns = @JoinColumn(name = "incident_id"))
    @Column(name = "linked_incident_id", length = 64)
    private Set<String> linkedIncidentIds = new HashSet<>();

    // SLA Tracker embedded/one-to-one
    @OneToOne(mappedBy = "incident", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private SlaTrackerJpaEntity slaTracker;

    // Notes
    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<InvestigationNoteJpaEntity> notes = new ArrayList<>();

    // Resolution
    @OneToOne(mappedBy = "incident", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private ResolutionJpaEntity resolution;

    // Feedback
    @OneToOne(mappedBy = "incident", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private FeedbackJpaEntity feedback;

    public IncidentJpaEntity() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }

    public IncidentCategory getCategory() { return category; }
    public void setCategory(IncidentCategory category) { this.category = category; }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public Double getCompositeScore() { return compositeScore; }
    public void setCompositeScore(Double compositeScore) { this.compositeScore = compositeScore; }

    public String getRulesApplied() { return rulesApplied; }
    public void setRulesApplied(String rulesApplied) { this.rulesApplied = rulesApplied; }

    public Instant getPriorityCalculatedAt() { return priorityCalculatedAt; }
    public void setPriorityCalculatedAt(Instant priorityCalculatedAt) { this.priorityCalculatedAt = priorityCalculatedAt; }

    public String getPriorityOverriddenBy() { return priorityOverriddenBy; }
    public void setPriorityOverriddenBy(String priorityOverriddenBy) { this.priorityOverriddenBy = priorityOverriddenBy; }

    public String getPriorityOverrideReason() { return priorityOverrideReason; }
    public void setPriorityOverrideReason(String priorityOverrideReason) { this.priorityOverrideReason = priorityOverrideReason; }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }

    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }

    public int getReopenCount() { return reopenCount; }
    public void setReopenCount(int reopenCount) { this.reopenCount = reopenCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Set<String> getLinkedIncidentIds() { return linkedIncidentIds; }
    public void setLinkedIncidentIds(Set<String> linkedIncidentIds) { this.linkedIncidentIds = linkedIncidentIds; }

    public SlaTrackerJpaEntity getSlaTracker() { return slaTracker; }
    public void setSlaTracker(SlaTrackerJpaEntity slaTracker) {
        this.slaTracker = slaTracker;
        if (slaTracker != null) {
            slaTracker.setIncident(this);
        }
    }

    public List<InvestigationNoteJpaEntity> getNotes() { return notes; }
    public void setNotes(List<InvestigationNoteJpaEntity> notes) { this.notes = notes; }

    public ResolutionJpaEntity getResolution() { return resolution; }
    public void setResolution(ResolutionJpaEntity resolution) {
        this.resolution = resolution;
        if (resolution != null) {
            resolution.setIncident(this);
        }
    }

    public FeedbackJpaEntity getFeedback() { return feedback; }
    public void setFeedback(FeedbackJpaEntity feedback) {
        this.feedback = feedback;
        if (feedback != null) {
            feedback.setIncident(this);
        }
    }
}

