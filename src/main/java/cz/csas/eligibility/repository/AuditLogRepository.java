package cz.csas.eligibility.repository;

import cz.csas.eligibility.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByApiNameAndCreatedAtBetween(
            String apiName,
            LocalDateTime from,
            LocalDateTime to
    );

    List<AuditLog> findByCorrelationId(String correlationId);

    List<AuditLog> findByRequestId(String requestId);

    @Query("SELECT a FROM AuditLog a WHERE a.success = false ORDER BY a.createdAt DESC")
    List<AuditLog> findFailedRequests();
}
