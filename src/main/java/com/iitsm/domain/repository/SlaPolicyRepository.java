package com.iitsm.domain.repository;

import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.SlaPolicy;
import java.util.List;
import java.util.Optional;

/**
 * Pattern: Repository.
 * Domain port for Service Level Agreement policy retrieval.
 */
public interface SlaPolicyRepository {

    SlaPolicy save(SlaPolicy policy);

    Optional<SlaPolicy> findById(String id);

    Optional<SlaPolicy> findByServiceIdAndPriority(String serviceId, Priority priority);

    Optional<SlaPolicy> findDefaultByPriority(Priority priority);

    List<SlaPolicy> findAll();
}
