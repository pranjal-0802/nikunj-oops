package com.iitsm.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "investigation_notes")
public class InvestigationNoteJpaEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private IncidentJpaEntity incident;

    @Column(name = "author_id", nullable = false, length = 64)
    private String authorId;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(name = "note_text", nullable = false, columnDefinition = "TEXT")
    private String noteText;

    @Column(name = "internal_only", nullable = false)
    private boolean internalOnly;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public InvestigationNoteJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public IncidentJpaEntity getIncident() { return incident; }
    public void setIncident(IncidentJpaEntity incident) { this.incident = incident; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getNoteText() { return noteText; }
    public void setNoteText(String noteText) { this.noteText = noteText; }

    public boolean isInternalOnly() { return internalOnly; }
    public void setInternalOnly(boolean internalOnly) { this.internalOnly = internalOnly; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
