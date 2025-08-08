package cz.csas.eligibility.config.auditlogs;

import cz.csas.eligibility.entity.AuditLog;
import cz.csas.eligibility.service.AuditLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestApiAuditFilter implements Filter {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        //conversion to HttpServletRequest/HttpServletResponse to access http-specific API (to access url, headers, methods, body...)
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Wrap request and response to cache their bodies (wrappers ensures that body can be read multiple times)
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);

        String requestId = UUID.randomUUID().toString();
        String correlationId = extractCorrelationId(requestWrapper);

        long startTime = System.currentTimeMillis();

        // Pre-load request body into cache regardless of method - preloadRequestBody() explicitly reads whole stream and saves it into cache
        preloadRequestBody(requestWrapper);

        try {
            //hands over the request to other filters up to the controller
            chain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;

            createAndSaveAuditLog(requestWrapper, responseWrapper, requestId, correlationId, executionTime);

            // copy response body back so that the client receives the expected HTTP response body in the response
            responseWrapper.copyBodyToResponse();
        }
    }

    private void preloadRequestBody(ContentCachingRequestWrapper request) {
        try {
            // Read entire body to fill cache
            byte[] buffer = new byte[request.getContentLength() > 0 ? request.getContentLength() : 0];
            if (buffer.length > 0) {
                request.getInputStream().read(buffer);
            }
        } catch (IOException e) {
            log.debug("Could not pre-load request body", e);
        }
    }

    private void createAndSaveAuditLog(ContentCachingRequestWrapper request,
                                       ContentCachingResponseWrapper response,
                                       String requestId,
                                       String correlationId,
                                       long executionTime) {
        try {
            String requestBody = extractRequestBody(request);
            String responseBody = extractResponseBody(response);
            String errorMessage = extractErrorMessage(responseBody);

            boolean isSuccess = response.getStatus() >= 200 && response.getStatus() < 300;

            AuditLog logEntry = AuditLog.builder()
                    .requestId(requestId)
                    .apiName("ApplicationServer")
                    .method(request.getMethod())
                    .url(request.getRequestURL().toString()
                            + (request.getQueryString() != null ? "?" + request.getQueryString() : ""))
                    .requestHeaders(formatHeaders(request))
                    .requestBody(requestBody)
                    .responseStatus(response.getStatus())
                    .responseHeaders(formatHeaders(response))
                    .responseBody(responseBody)
                    .executionTimeMs(executionTime)
                    .success(isSuccess)
                    .errorMessage(errorMessage)
                    .correlationId(correlationId)
                    .userId(extractUserId(request))
                    .build();

            auditLogService.saveAuditLog(logEntry);

            log.info("Audit log: REST API call completed with result {} - RequestId={} Status={} Duration={}ms",
                    isSuccess? "SUCCESS" : "FAILURE",
                    requestId, response.getStatus(), executionTime);

        } catch (Exception e) {
            log.error("Audit log: Failed to create and save REST API audit log for RequestId={}, CorrelationId={}", requestId, correlationId, e);
        }
    }

    private String extractRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        String body = new String(content, StandardCharsets.UTF_8);
        return body.length() > 10000 ? body.substring(0, 10000) + "... [TRUNCATED]" : body;
    }

    private String extractResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        String body = new String(content, StandardCharsets.UTF_8);
        return body.length() > 10000 ? body.substring(0, 10000) + "... [TRUNCATED]" : body;
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            if (node.has("errorMessage")) {
                return node.get("errorMessage").asText();
            }
        } catch (IOException e) {
            log.debug("Could not parse errorMessage from response body", e);
        }
        return null;
    }

    private String formatHeaders(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder().append("[");;
        request.getHeaderNames().asIterator()
                .forEachRemaining(h -> sb.append(h).append(":").append("\"").append(request.getHeader(h)).append("\"").append("; "));

        if (sb.length() > 2 && sb.substring(sb.length() - 2).equals("; ")) {
            sb.delete(sb.length() - 2, sb.length());
        }

        return sb.append("]").toString();
    }

    private String formatHeaders(HttpServletResponse response) {
        StringBuilder sb = new StringBuilder().append("[");
        response.getHeaderNames()
                .forEach(h -> sb.append(h).append(":").append("\"").append(response.getHeader(h)).append("\"").append("; "));

        if (sb.length() > 2 && sb.substring(sb.length() - 2).equals("; ")) {
            sb.delete(sb.length() - 2, sb.length());
        }

        return sb.append("]").toString();
    }

    private String extractCorrelationId(ContentCachingRequestWrapper request) {
        return Objects.nonNull(request.getHeader("correlation-id"))
                ? request.getHeader("correlation-id")
                : null;
    }

    private String extractUserId(ContentCachingRequestWrapper request) {
        // Implemented logic for extraction of user ID from JWT or from session
        // We have no such logic now, therefore SYSTEM
        return "SYSTEM";
    }
}
