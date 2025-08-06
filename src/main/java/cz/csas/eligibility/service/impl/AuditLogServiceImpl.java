package cz.csas.eligibility.service.impl;

import cz.csas.eligibility.entity.AuditLog;
import cz.csas.eligibility.repository.AuditLogRepository;
import cz.csas.eligibility.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async
    @Transactional
    public void saveAuditLog(AuditLog auditLog) {
        try {
            // limit logged request/response body length to some reasonable value
            if (auditLog.getRequestBody() != null && auditLog.getRequestBody().length() > 10000) {
                auditLog.setRequestBody(auditLog.getRequestBody().substring(0, 10000) + "... [TRUNCATED]");
            }
            if (auditLog.getResponseBody() != null && auditLog.getResponseBody().length() > 10000) {
                auditLog.setResponseBody(auditLog.getResponseBody().substring(0, 10000) + "... [TRUNCATED]");
            }

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved with requestId: {}", auditLog.getRequestId());
        } catch (Exception e) {
            log.error("Failed to save audit log for requestId: {}", auditLog.getRequestId(), e);
        }
    }
}
