package cz.csas.eligibility.service;

import cz.csas.eligibility.entity.AuditLog;

public interface AuditLogService {
    void saveAuditLog(AuditLog auditLog);
}
