package com.apisentinel.repository;

import com.apisentinel.entity.ApiCheckHistory;
import com.apisentinel.entity.CheckStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiCheckHistoryRepository extends JpaRepository<ApiCheckHistory, Long> {
    List<ApiCheckHistory> findByApiIdOrderByCheckedAtDesc(Long apiId);

    List<ApiCheckHistory> findTop10ByApiIdOrderByCheckedAtDesc(Long apiId);

    Optional<ApiCheckHistory> findTopByApiIdOrderByCheckedAtDesc(Long apiId);

    long countByApiIdAndStatus(Long apiId, CheckStatus status);

    long countByApiId(Long apiId);

    void deleteByApiId(Long apiId);

    @Query("SELECT AVG(c.responseTimeMs) FROM ApiCheckHistory c WHERE c.api.id = ?1 AND c.status = 'UP'")
    Double getAverageResponseTime(Long apiId);
}
