package com.iitsm.domain.model;

import com.iitsm.domain.exception.ValidationException;
import com.iitsm.domain.state.IncidentStates;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * OOP: Aggregate Root representing an IT service incident ticket.
 * Encapsulates full ticket lifecycle, invariants, and delegates lifecycle actions to IncidentState.
 * Private fields, no public setters, domain invariants enforced in constructor and methods.
 */
public class Incident {

    private final String id;
    private final String title;
    private final String description;
    private final String reporterId;
    private final Instant createdAt;

    private IncidentCategory category;
    private String serviceId;
    private PriorityDecision priorityDecision;
    private IncidentStatus status;
    private String assigneeId;
    private SlaTracker slaTracker; // Composition: lifecycle bound to this incident

    private final List<InvestigationNote> notes;
    private Resolution resolution;
    private Feedback feedback;
    private int reopenCount;
    private final Set<String> linkedIncidentIds;
    private Long version; // Optimistic locking

    public Incident(String id, String title, String description, String reporterId, Instant createdAt) {
        if (id == null || id.trim().isEmpty()) {
            throw new ValidationException("id", "Incident ID cannot be blank");
        }
        if (title == null || title.trim().length() < 5 || title.trim().length() > 120) {
            throw new ValidationException("title", "Title length must be between 5 and 120 characters");
        }
        if (description == null || description.trim().length() < 20 || description.trim().length() > 4000) {
            throw new ValidationException("description", "Description length must be between 20 and 4000 characters");
        }
        if (reporterId == null || reporterId.trim().isEmpty()) {
            throw new ValidationException("reporterId", "Reporter ID cannot be blank");
        }
        if (createdAt == null) {
            throw new ValidationException("createdAt", "Created timestamp cannot be null");
        }

        this.id = id.trim();
        this.title = sanitizeHtml(title.trim());
        this.description = sanitizeHtml(description.trim());
        this.reporterId = reporterId.trim();
        this.createdAt = createdAt;

        this.status = IncidentStatus.NEW;
        this.notes = new ArrayList<>();
        this.linkedIncidentIds = new HashSet<>();
        this.reopenCount = 0;
        this.version = 0L;
    }

    // --- State Pattern Delegation Methods ---

    public void classify(
            IncidentCategory category,
            String serviceId,
            PriorityDecision priorityDecision,
            SlaPolicy slaPolicy,
            Instant now) {
        Objects.requireNonNull(slaPolicy, "slaPolicy cannot be null");
        IncidentStates.of(this.status).classify(this, category, serviceId, priorityDecision, now);
        this.slaTracker = SlaTracker.create(this.createdAt, slaPolicy);
    }

    public void assign(String technicianId, Instant now) {
        IncidentStates.of(this.status).assign(this, technicianId, now);
    }

    public void acknowledge(String technicianId, Instant now) {
        IncidentStates.of(this.status).acknowledge(this, technicianId, now);
    }

    public void startInvestigation(String technicianId, Instant now) {
        IncidentStates.of(this.status).startInvestigation(this, technicianId, now);
    }

    public void requestInfo(String technicianId, String message, Instant now) {
        IncidentStates.of(this.status).requestInfo(this, technicianId, message, now);
    }

    public void provideInfo(String employeeId, String responseText, Instant now) {
        IncidentStates.of(this.status).provideInfo(this, employeeId, responseText, now);
    }

    public void resolve(Resolution resolution, Instant now) {
        IncidentStates.of(this.status).resolve(this, resolution, now);
    }

    public void verify(String employeeId, Instant now) {
        IncidentStates.of(this.status).verify(this, employeeId, now);
    }

    public void reject(String employeeId, String reason, Instant now) {
        IncidentStates.of(this.status).reject(this, employeeId, reason, now);
    }

    public void close(String actorId, Feedback feedback, Instant now) {
        IncidentStates.of(this.status).close(this, actorId, feedback, now);
    }

    public void flagWrongDomain(String technicianId, String reason, Instant now) {
        IncidentStates.of(this.status).flagWrongDomain(this, technicianId, reason, now);
    }

    // --- Domain Package Mutations (Invoked by IncidentState implementations) ---

    public void transitionTo(IncidentStatus nextStatus) {
        this.status = Objects.requireNonNull(nextStatus, "Next status cannot be null");
    }

    public void setClassificationData(
            IncidentCategory category,
            String serviceId,
            PriorityDecision priorityDecision) {
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.serviceId = serviceId;
        this.priorityDecision = Objects.requireNonNull(priorityDecision, "PriorityDecision cannot be null");
    }

    public void setAssignee(String technicianId) {
        this.assigneeId = Objects.requireNonNull(technicianId, "assigneeId cannot be null");
    }

    public void clearAssignee() {
        this.assigneeId = null;
    }

    public void setResolution(Resolution resolution) {
        this.resolution = Objects.requireNonNull(resolution, "resolution cannot be null");
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    public void incrementReopenCount() {
        this.reopenCount++;
    }

    public void addNote(InvestigationNote note) {
        Objects.requireNonNull(note, "note cannot be null");
        this.notes.add(note);
    }

    public void linkIncident(String duplicateIncidentId) {
        if (duplicateIncidentId != null && !duplicateIncidentId.equals(this.id)) {
            this.linkedIncidentIds.add(duplicateIncidentId);
        }
    }

    public void overridePriority(Priority newPriority, String adminId, String reason, Instant now) {
        if (this.priorityDecision == null) {
            throw new ValidationException("priorityDecision", "Cannot override priority before classification");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException("reason", "Mandatory reason required for priority override");
        }
        this.priorityDecision = this.priorityDecision.withOverride(newPriority, adminId, reason.trim(), now);
    }

    // --- Invariant & HTML Escaping Helpers ---

    private static String sanitizeHtml(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    // --- Getters returning immutable views (Section 2 Rule 10) ---

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getReporterId() {
        return reporterId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public IncidentCategory getCategory() {
        return category;
    }

    public String getServiceId() {
        return serviceId;
    }

    public PriorityDecision getPriorityDecision() {
        return priorityDecision;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public SlaTracker getSlaTracker() {
        return slaTracker;
    }

    public List<InvestigationNote> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    public Resolution getResolution() {
        return resolution;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public int getReopenCount() {
        return reopenCount;
    }

    public Set<String> getLinkedIncidentIds() {
        return Collections.unmodifiableSet(linkedIncidentIds);
    }

    public Long getVersion() {
        return version;
    }

    public Set<String> getAllowedActions() {
        return IncidentStates.of(this.status).allowedActions();
    }
}
