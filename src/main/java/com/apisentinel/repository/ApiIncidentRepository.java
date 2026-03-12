package com.apisentinel.repository;

import com.apisentinel.entity.ApiIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiIncidentRepository extends JpaRepository<ApiIncident, Long> {
    List<ApiIncident> findByApiIdOrderByTimestampDesc(Long apiId);
    long countByApiId(Long apiId);
    void deleteByApiId(Long apiId);
}
