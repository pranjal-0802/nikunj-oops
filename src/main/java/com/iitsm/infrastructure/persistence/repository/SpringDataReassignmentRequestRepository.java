package com.iitsm.infrastructure.persistence.repository;

import com.iitsm.domain.model.ReassignmentRequest;
import com.iitsm.infrastructure.persistence.entity.ReassignmentRequestJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for persisting and querying technician reassignment requests.
 */
@Repository
public interface SpringDataReassignmentRequestRepository extends JpaRepository<ReassignmentRequestJpaEntity, String> {

    List<ReassignmentRequestJpaEntity> findByStatus(ReassignmentRequest.Status status);

    List<ReassignmentRequestJpaEntity> findByIncidentId(String incidentId);
}

