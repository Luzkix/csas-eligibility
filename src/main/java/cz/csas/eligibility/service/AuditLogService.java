package cz.csas.eligibility.service;

import cz.csas.eligibility.entity.AuditLog;

import java.util.concurrent.CompletableFuture;

/**
 * Service interface for saving AuditLog operations.
 */
public interface AuditLogService {
    /**
     * Function saves audit log regarding performed api calls asynchronously.
     *
     * @param auditLog      AuditLog object
     * @return non-null CompletableFuture<AuditLog> if save was processed correctly or null in case of any error
     */
    CompletableFuture<AuditLog> saveAuditLog(AuditLog auditLog);
}
