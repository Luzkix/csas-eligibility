package cz.csas.eligibility.controller;

import cz.csas.eligibility.entity.AuditLog;
import cz.csas.eligibility.entity.Eligibility;
import cz.csas.eligibility.exceptions.ExternalApiException;
import cz.csas.eligibility.model.*;
import cz.csas.eligibility.repository.AuditLogRepository;
import cz.csas.eligibility.repository.EligibilityRepository;
import cz.csas.eligibility.service.ApiServiceAccounts;
import cz.csas.eligibility.service.ApiServiceClients;
import cz.csas.eligibility.testutil.EligibilityTestDataBuilder;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EligibilityControllerIT {

    private static final String URL = "/api/v1/eligibility";
    private static final String CLIENT_ID = "1234-56-78-90.12.34.567890";
    private static final String CORR_ID = "57fe7696-6151-4ecc-ab8b-8840e3872185";

    @Autowired private MockMvc mockMvc;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private EligibilityRepository eligibilityRepository;
    @Autowired private ApiServiceAccounts apiServiceAccounts;
    @Autowired private ApiServiceClients apiServiceClients;

    private List<Account> accounts;
    private GetClientDetailResponse adult;
    private GetClientDetailResponse minor;

    @BeforeEach
    void init() {
        auditLogRepository.deleteAll();
        eligibilityRepository.deleteAll();
        reset(apiServiceAccounts, apiServiceClients);

        accounts = EligibilityTestDataBuilder.createAccountsWithNationalAccount();
        adult = EligibilityTestDataBuilder.createDefaultAdultClient();
        minor = EligibilityTestDataBuilder.createDefaultMinorClient();
    }

    /* ---------- 1a – accounts>0 & adult => ELIGIBLE ---------- */
    @Test
    void isAdultAndHasAccount() throws Exception {
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORR_ID)).thenReturn(accounts);
        when(apiServiceClients.getClientDetail (CLIENT_ID, CORR_ID)).thenReturn(adult);

        mockMvc.perform(get(URL)
                        .header("clientId", CLIENT_ID)
                        .header("correlation-id", CORR_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("correlation-id", CORR_ID))
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.reasons").isEmpty());

        //rest api audit logs checking
        checkRestApiAuditLogs(true,200);

        //eligibility logs checking
        checkEligibilityResultLogs(Eligibility.EligibilityResultEnum.ELIGIBLE);
    }

    /* ---------- 1b – no accounts & adult => NOT_ELIGIBLE (NO_ACCOUNT) ---------- */
    @Test
    void noAccountAdult() throws Exception {
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORR_ID)).thenReturn(Collections.emptyList());
        when(apiServiceClients.getClientDetail (CLIENT_ID, CORR_ID)).thenReturn(adult);

        mockMvc.perform(get(URL)
                        .header("clientId", CLIENT_ID)
                        .header("correlation-id", CORR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.reasons[0]").value("NO_ACCOUNT"));

        //rest api audit logs checking
        checkRestApiAuditLogs(true,200);

        //eligibility logs checking
        checkEligibilityResultLogs(Eligibility.EligibilityResultEnum.NOT_ELIGIBLE);
    }

    /* ---------- 1c – accounts>0 & minor => NOT_ELIGIBLE (NO_ADULT) ---------- */
    @Test
    void hasAccountMinor() throws Exception {
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORR_ID)).thenReturn(accounts);
        when(apiServiceClients.getClientDetail (CLIENT_ID, CORR_ID)).thenReturn(minor);

        mockMvc.perform(get(URL)
                        .header("clientId", CLIENT_ID)
                        .header("correlation-id", CORR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.reasons").value("NO_ADULT"));

        //rest api audit logs checking
        checkRestApiAuditLogs(true,200);

        //eligibility logs checking
        checkEligibilityResultLogs(Eligibility.EligibilityResultEnum.NOT_ELIGIBLE);
    }

    /* ---------- 1d – no accounts & minor => NOT_ELIGIBLE (both reasons) ---------- */
    @Test
    void noAccountMinor() throws Exception {
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORR_ID)).thenReturn(Collections.emptyList());
        when(apiServiceClients.getClientDetail (CLIENT_ID, CORR_ID)).thenReturn(minor);

        mockMvc.perform(get(URL)
                        .header("clientId", CLIENT_ID)
                        .header("correlation-id", CORR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.reasons").value(containsInAnyOrder("NO_ACCOUNT","NO_ADULT")));

        //rest api audit logs checking
        checkRestApiAuditLogs(true,200);

        //eligibility logs checking
        checkEligibilityResultLogs(Eligibility.EligibilityResultEnum.NOT_ELIGIBLE);
    }

    /* ---------- 1e – external API error => BAD_REQUEST & result ERROR ---------- */
    @Test
    void externalApiError() throws Exception {
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORR_ID))
                .thenThrow(new ExternalApiException("External API not working",new RuntimeException("x")));

        mockMvc.perform(get(URL)
                        .header("clientId", CLIENT_ID)
                        .header("correlation-id", CORR_ID))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("correlation-id", CORR_ID))
                .andExpect(jsonPath("$.errorStatusValue").value(400))
                .andExpect(jsonPath("$.errorStatus").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.errorTime").isNotEmpty())
                .andExpect(jsonPath("$.errorMessage").value("External API not working"));

        //rest api audit logs checking
        checkRestApiAuditLogs(false,400);

        //eligibility logs checking
        checkEligibilityResultLogs(Eligibility.EligibilityResultEnum.ERROR);
    }

    /* ---------- 2a – missing/invalid header => validation error ---------- */
    @Test
    void missingClientIdHeader() throws Exception {
        mockMvc.perform(get(URL).header("correlation-id", CORR_ID))
                .andExpect(status().isBadRequest());


        //rest api audit logs checking
        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).hasSize(1);
        AuditLog log = logs.get(0);
        assertThat(log.getSuccess()).isEqualTo(false);
        assertThat(log.getResponseStatus()).isEqualTo(400);
        assertThat(log.getMethod()).isEqualTo("GET");
        assertThat(log.getUrl()).contains(URL);
        assertThat(log.getRequestHeaders()).contains(CORR_ID);
        assertThat(log.getCorrelationId()).isEqualTo(CORR_ID);
        assertThat(log.getErrorMessage()).isNull();
        assertThat(log.getExceptionName()).isNull();
        assertThat(log.getApiName()).isEqualTo("ApplicationServer");
        assertThat(log.getUserId()).isEqualTo("SYSTEM");
        assertThat(log.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());

        //eligibility logs checking
        assertThat(eligibilityRepository.findAll()).isEmpty();
    }

    /* ---------------- helpers ---------------- */
    private void checkRestApiAuditLogs(boolean success, int status){
        List<AuditLog> logs = auditLogRepository.findByCorrelationId(CORR_ID);
        assertThat(logs).hasSize(1);
        AuditLog log = logs.get(0);
        assertThat(log.getSuccess()).isEqualTo(success);
        assertThat(log.getResponseStatus()).isEqualTo(status);
        assertThat(log.getApiName()).isEqualTo("ApplicationServer");
        assertThat(log.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }
    private void checkEligibilityResultLogs(Eligibility.EligibilityResultEnum expected){
        List<Eligibility> eligibilityResults = eligibilityRepository.findByClientId(CLIENT_ID);
        assertThat(eligibilityResults).hasSize(1);
        assertThat(eligibilityResults.get(0).getResult()).isEqualTo(expected);
    }

    //CONFIG - mocking external api services.
    // Note: @Primary solved the issue when these beans were influencing also other IT tests
    // (spring was detecting multiple beans for e.g. ApiServiceAccounts - one of which was from this class. @Primary solved it)
    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public ApiServiceAccounts apiServiceAccounts() {
            return Mockito.mock(ApiServiceAccounts.class);
        }
        @Bean
        @Primary
        public ApiServiceClients apiServiceClients() {
            return Mockito.mock(ApiServiceClients.class);
        }
    }
}
