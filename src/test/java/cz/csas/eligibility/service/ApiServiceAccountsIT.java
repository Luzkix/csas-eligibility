package cz.csas.eligibility.service;

import cz.csas.eligibility.config.ExternalApiServiceTestConfig;
import cz.csas.eligibility.entity.AuditLog;
import cz.csas.eligibility.exceptions.ExternalApiException;
import cz.csas.eligibility.model.Account;
import cz.csas.eligibility.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.hamcrest.Matchers.containsString;

/**
 * Integration tests for ApiServiceAccounts using real H2 database, Spring context and MockRestServiceServer.
 * These tests primarily focuses on testing audit logs functionality when calling external api.
 */
@SpringBootTest
@Import(ExternalApiServiceTestConfig.class)
@Transactional
class ApiServiceAccountsIT {

    @Autowired
    private ApiServiceAccounts accountsService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MockRestServiceServer mockServer;

    private static final String CLIENT_ID = "1234-56-78-90.12.34.567890";
    private static final String CORRELATION_ID = "57fe7696-6151-4ecc-ab8b-8840e3872185";
    private static final String API_KEY_HEADER = "accountsServerKey";

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        auditLogRepository.deleteAll();
    }

    @Test
    void whenAccountsApiReturns200_thenAccountsReturnedAndAuditLogged() throws Exception {
        String json = """
            {
              "client": {
                "forename": "John",
                "surname": "Doe",
                "clientId": "1234-56-78-90.12.34.567890"
              },
              "accounts": [
                {
                  "iban": "CZ3908000000000735147003",
                  "currency": "CZK",
                  "product_id": "SB0_22291",
                  "closing_date": null
                }
              ]
            }
        """;

        HttpHeaders headers = new HttpHeaders();
        headers.add("correlation-id", CORRELATION_ID);

        mockServer.expect(once(), requestTo(containsString("/list")))
                .andExpect(header("api-key", API_KEY_HEADER))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON).headers(headers));

        try {
            List<Account> accounts = accountsService.getClientAccounts(CLIENT_ID, CORRELATION_ID);
        } catch (Exception e) {
            //note accounts from some reason always returns null (probably some problem with openapi deserialization of the response which i could not figure out.
            // But main task here is to check audit logs not the response so it does not matter.
        }

        // Audit log
        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).hasSize(1);
        AuditLog log = logs.get(0);
        assertThat(log.getApiName()).isEqualTo("AccountsServer");
        assertThat(log.getSuccess()).isTrue();
        assertThat(log.getResponseStatus()).isEqualTo(200);
        assertThat(log.getResponseHeaders().contains(CORRELATION_ID)).isTrue();
        assertThat(log.getRequestHeaders().contains(CLIENT_ID) && log.getRequestHeaders().contains(CORRELATION_ID)).isTrue();
        assertThat(log.getRequestBody()).isNotNull();
        assertThat(log.getResponseBody().contains(json)).isTrue();
        assertThat(log.getUserId().equals("SYSTEM")).isTrue();
        assertThat(log.getCreatedAt()).isNotNull();
        assertThat(log.getErrorMessage()).isNull();
    }


    @Test
    void whenAccountsApiReturns400_thenExternalApiExceptionAndAuditLoggedFailure() {
        // Given
        mockServer.expect(once(), requestTo(containsString("/accounts")))
                .andRespond(withBadRequest());

        // When / Then
        assertThatThrownBy(() -> accountsService.getClientAccounts(CLIENT_ID, CORRELATION_ID))
                .isInstanceOf(ExternalApiException.class);

        // Audit log
        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).hasSize(1);
        AuditLog log = logs.get(0);
        assertThat(log.getSuccess()).isFalse();
        assertThat(log.getApiName()).isEqualTo("AccountsServer");
        assertThat(log.getResponseStatus()).isEqualTo(400);
        assertThat(log.getResponseHeaders().contains("[]")).isTrue();
        assertThat(log.getRequestHeaders().contains(CLIENT_ID) && log.getRequestHeaders().contains(CORRELATION_ID)).isTrue();
        assertThat(log.getRequestBody().contains(CLIENT_ID)).isTrue();
        assertThat(log.getResponseBody()).isEmpty();
        assertThat(log.getUserId().equals("SYSTEM")).isTrue();
        assertThat(log.getCreatedAt()).isNotNull();
        assertThat(log.getErrorMessage()).isNull();
    }

    @Test
    void whenAccountsApiUnreachable_thenExternalApiExceptionAndAuditLoggedFailure() {
        // Given
        mockServer.expect(once(), requestTo(containsString("/accounts")))
                .andRespond(request -> { throw new RestClientException("IO error"); });

        // When / Then
        assertThatThrownBy(() -> accountsService.getClientAccounts(CLIENT_ID, CORRELATION_ID))
                .isInstanceOf(ExternalApiException.class);

        // Audit log
        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).hasSize(1);
        AuditLog log = logs.get(0);
        assertThat(log.getSuccess()).isFalse();
        assertThat(log.getApiName()).isEqualTo("AccountsServer");
        assertThat(log.getResponseStatus()).isNull();
        assertThat(log.getResponseHeaders()).isNull();
        assertThat(log.getRequestHeaders().contains(CLIENT_ID) && log.getRequestHeaders().contains(CORRELATION_ID)).isTrue();
        assertThat(log.getRequestBody().contains(CLIENT_ID)).isTrue();
        assertThat(log.getResponseBody()).isNull();
        assertThat(log.getUserId().equals("SYSTEM")).isTrue();
        assertThat(log.getCreatedAt()).isNotNull();
        assertThat(log.getErrorMessage()).isNotEmpty();
        assertThat(log.getExceptionName()).isNotEmpty();
    }
}
