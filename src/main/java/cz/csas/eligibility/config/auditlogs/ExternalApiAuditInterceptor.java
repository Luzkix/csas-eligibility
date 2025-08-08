package cz.csas.eligibility.config.auditlogs;

import cz.csas.eligibility.entity.AuditLog;
import cz.csas.eligibility.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalApiAuditInterceptor implements ClientHttpRequestInterceptor {

    private final AuditLogService auditLogService;

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        // Assembly audit log record for the request
        AuditLog.AuditLogBuilder auditLogBuilder = AuditLog.builder()
                .requestId(requestId)
                .apiName(determineApiName(request.getURI().getHost()))
                .method(request.getMethod().name())
                .url(request.getURI().toString())
                .requestHeaders(request.getHeaders().toString())
                .requestBody(body.length > 0 ? new String(body, StandardCharsets.UTF_8) : null)
                .correlationId(extractCorrelationId(request))
                .userId(extractUserId(request));

        ClientHttpResponse response = null;
        try {
            // execute request
            response = execution.execute(request, body);

            // read response
            String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);

            long executionTime = System.currentTimeMillis() - startTime;
            boolean isSuccess = response.getStatusCode().is2xxSuccessful();

            // assembly audit log record for response
            AuditLog auditLog = auditLogBuilder
                    .responseStatus(response.getStatusCode().value())
                    .responseHeaders(response.getHeaders().toString())
                    .responseBody(responseBody)
                    .executionTimeMs(executionTime)
                    .success(isSuccess)
                    .build();

            // save audit log to DB (asynchronously)
            auditLogService.saveAuditLog(auditLog);

            log.info("Audit log: External API call completed with result {} - RequestId: {}, CorrelationId: {}, API: {}, Status: {}, Duration: {}ms",
                    isSuccess? "SUCCESS" : "FAILURE",
                    requestId, extractCorrelationId(request), auditLog.getApiName(), response.getStatusCode().value(), executionTime);

            return response;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // assembly audit log record in case of error
            AuditLog auditLog = auditLogBuilder
                    .executionTimeMs(executionTime)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .exceptionName(e.getClass().getName())
                    .build();

            auditLogService.saveAuditLog(auditLog);

            log.error("Audit log: External API call failed - RequestId: {}, CorrelationId: {}, API: {}, Duration: {}ms, Error type: {}, Error message: {}",
                    requestId, extractCorrelationId(request), auditLog.getApiName(), executionTime, e.getClass().getName(), e.getMessage());

            throw e;
        }
    }

    private String determineApiName(String host) {
        if (host != null) {
            if (host.contains("accounts.cluster.domain.cz")) {
                return "AccountsServer";
            } else if (host.contains("clients.cluster.domain.cz")) {
                return "ClientsServer";
            }
        }
        return "Unknown";
    }

    private String extractCorrelationId(HttpRequest request) {
        return request.getHeaders().getFirst("correlation-id");
    }

    private String extractUserId(HttpRequest request) {
        // Implemented logic for extraction of user ID from JWT or from session
        // We have no such logic now, therefore SYSTEM
        return "SYSTEM";
    }
}