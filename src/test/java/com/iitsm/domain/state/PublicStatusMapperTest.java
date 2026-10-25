package com.iitsm.domain.state;

import static org.assertj.core.api.Assertions.assertThat;

import com.iitsm.domain.model.IncidentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * OOP: Verifies that internal states collapse accurately into the employee 5-step stepper.
 */
class PublicStatusMapperTest {

    @ParameterizedTest
    @EnumSource(IncidentStatus.class)
    @DisplayName("Every internal status maps to a valid public stepper state")
    void testAllStatesCovered(IncidentStatus status) {
        PublicStatusMapper.PublicStatus publicStatus = PublicStatusMapper.toPublicStatus(status);
        assertThat(publicStatus).isNotNull();
        assertThat(publicStatus.getStepNumber()).isBetween(1, 5);
        assertThat(publicStatus.getDisplayName()).isNotBlank();
    }

    @Test
    @DisplayName("Verification of specific mapping projections")
    void testSpecificMappings() {
        assertThat(PublicStatusMapper.toPublicStatus(IncidentStatus.NEW))
                .isEqualTo(PublicStatusMapper.PublicStatus.NEW);
        assertThat(PublicStatusMapper.toPublicStatus(IncidentStatus.CLASSIFIED))
                .isEqualTo(PublicStatusMapper.PublicStatus.NEW);

        assertThat(PublicStatusMapper.toPublicStatus(IncidentStatus.ASSIGNED))
                .isEqualTo(PublicStatusMapper.PublicStatus.ASSIGNED);
        assertThat(PublicStatusMapper.toPublicStatus(IncidentStatus.ACKNOWLEDGED))
                .isEqualTo(PublicStatusMapper.PublicStatus.ASSIGNED);

        assertThat(PublicStatusMapper.toPublicStatus(IncidentStatus.IN_PROGRESS))
                .isEqualTo(PublicStatusMapper.PublicStatus.IN_PROGRESS);
        assertThat(PublicStatusMapper.toPublicStatus(IncidentStatus.PENDING_INFO))
                .isEqualTo(PublicStatusMapper.PublicStatus.IN_PROGRESS);

        assertThat(PublicStatusMapper.toPublicStatus(IncidentStatus.RESOLVED))
                .isEqualTo(PublicStatusMapper.PublicStatus.RESOLVED);
        assertThat(PublicStatusMapper.toPublicStatus(IncidentStatus.VERIFIED))
                .isEqualTo(PublicStatusMapper.PublicStatus.RESOLVED);

        assertThat(PublicStatusMapper.toPublicStatus(IncidentStatus.CLOSED))
                .isEqualTo(PublicStatusMapper.PublicStatus.CLOSED);
    }
}

