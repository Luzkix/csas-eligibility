package cz.csas.eligibility.interceptor;

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
public class AuditLoggingInterceptor implements ClientHttpRequestInterceptor {

    private final AuditLogService auditLogService;

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        // Sestavení auditního záznamu pro request
        AuditLog.AuditLogBuilder auditLogBuilder = AuditLog.builder()
                .requestId(requestId)
                .apiName(determineApiName(request.getURI().getHost()))
                .method(request.getMethod().name())
                .url(request.getURI().toString())
                .requestHeaders(request.getHeaders().toString())
                .requestBody(body.length > 0 ? new String(body, StandardCharsets.UTF_8) : null)
                .correlationId(extractCorrelationId(request))
                .userId("SYSTEM"); //mohlo by zde byt id uzivatele pokud by se eligibility proverovalo rucne treba poradcem z frontendu

        ClientHttpResponse response = null;
        try {
            // Vykonání požadavku
            response = execution.execute(request, body);

            // Čtení response
            String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);

            long executionTime = System.currentTimeMillis() - startTime;

            // Dokončení auditního záznamu
            AuditLog auditLog = auditLogBuilder
                    .responseStatus(response.getStatusCode().value())
                    .responseHeaders(response.getHeaders().toString())
                    .responseBody(responseBody)
                    .executionTimeMs(executionTime)
                    .success(response.getStatusCode().is2xxSuccessful())
                    .build();

            // Asynchronní uložení do DB
            auditLogService.saveAuditLog(auditLog);

            log.info("Audit log: API call completed - RequestId: {}, CorrelationId: {}, API: {}, Status: {}, Duration: {}ms",
                    requestId, extractCorrelationId(request), auditLog.getApiName(), response.getStatusCode().value(), executionTime);

            return response;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Auditní záznam pro chybu
            AuditLog auditLog = auditLogBuilder
                    .executionTimeMs(executionTime)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .exceptionName(e.getClass().getName())
                    .build();

            auditLogService.saveAuditLog(auditLog);

            log.error("Audit log: API call failed - RequestId: {}, CorrelationId: {}, API: {}, Duration: {}ms, Error type: {}, Error message: {}",
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
            } else if (host.contains("localhost")) {
                return "ApplicationServer";
            }
        }
        return "Unknown";
    }

    private String extractCorrelationId(HttpRequest request) {
        return request.getHeaders().getFirst("correlation-id");
    }
}