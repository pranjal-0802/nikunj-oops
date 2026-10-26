package com.iitsm.domain.repository;

import com.iitsm.domain.model.ReassignmentRequest;
import java.util.List;
import java.util.Optional;

/**
 * Domain port for technician wrong-domain / out-of-scope reassignment requests.
 */
public interface ReassignmentRequestRepository {
    ReassignmentRequest save(ReassignmentRequest request);
    Optional<ReassignmentRequest> findById(String id);
    List<ReassignmentRequest> findByIncidentId(String incidentId);
    List<ReassignmentRequest> findByStatus(ReassignmentRequest.Status status);
    List<ReassignmentRequest> findAll();
}

