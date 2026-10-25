package com.iitsm.infrastructure.persistence.entity;

import com.iitsm.domain.model.IncidentCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;

@Embeddable
public class TechnicianSkillEmbeddable {

    @Column(name = "skill_name", nullable = false, length = 128)
    private String skillName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private IncidentCategory category;

    public TechnicianSkillEmbeddable() {}

    public TechnicianSkillEmbeddable(String skillName, IncidentCategory category) {
        this.skillName = skillName;
        this.category = category;
    }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public IncidentCategory getCategory() { return category; }
    public void setCategory(IncidentCategory category) { this.category = category; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TechnicianSkillEmbeddable that = (TechnicianSkillEmbeddable) o;
        return Objects.equals(skillName, that.skillName) && category == that.category;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skillName, category);
    }
}

