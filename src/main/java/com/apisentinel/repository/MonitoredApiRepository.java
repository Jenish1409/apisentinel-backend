package com.apisentinel.repository;

import com.apisentinel.entity.MonitoredApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonitoredApiRepository extends JpaRepository<MonitoredApi, Long> {
    List<MonitoredApi> findByUserId(Long userId);

    /**
     * Fetch all monitored APIs with their User eagerly loaded in a single JOIN query.
     * This prevents LazyInitializationException when the User is accessed on a
     * Reactor/Netty IO thread (outside the JPA session) in MonitoringEngine.
     */
    @Query("SELECT ma FROM MonitoredApi ma JOIN FETCH ma.user")
    List<MonitoredApi> findAllWithUser();
}
