package com.iitsm.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "feedback")
public class FeedbackJpaEntity {

    @Id
    @Column(name = "incident_id", length = 64)
    private String incidentId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "incident_id")
    private IncidentJpaEntity incident;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    public FeedbackJpaEntity() {}

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public IncidentJpaEntity getIncident() { return incident; }
    public void setIncident(IncidentJpaEntity incident) { this.incident = incident; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
}
