package cz.csas.eligibility.service;

import cz.csas.eligibility.entity.Eligibility;
import cz.csas.eligibility.exceptions.EligibilityApiException;
import cz.csas.eligibility.exceptions.ExternalApiException;
import cz.csas.eligibility.model.Account;
import cz.csas.eligibility.model.GetClientDetailResponse;
import cz.csas.eligibility.model.GetEligibilityResponse;
import cz.csas.eligibility.model.NationalAccount;
import cz.csas.eligibility.repository.EligibilityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Integration tests for EligibilityService using real H2 database and Spring context.
 * These tests verify the complete flow including database operations.
 */
@SpringBootTest
@Transactional
@Import(EligibilityServiceIT.TestConfig.class)
class EligibilityServiceIT {

    @Autowired
    private EligibilityService eligibilityService;

    @Autowired
    private EligibilityRepository eligibilityRepository;

    @Autowired
    private ApiServiceAccounts apiServiceAccounts;

    @Autowired
    private ApiServiceClients apiServiceClients;

    private static final String CLIENT_ID = "1234-56-78-90.12.34.567890";
    private static final String CORRELATION_ID = "integration-test-correlation-id";
    private static final String ADULT_BIRTH_DATE = "1990-01-01";
    private static final String MINOR_BIRTH_DATE = "2010-01-01";

    private GetClientDetailResponse adultClientDetail;
    private GetClientDetailResponse minorClientDetail;
    private List<Account> accountsWithData;

    @BeforeEach
    void setUp() {
        // Clear database before each test
        eligibilityRepository.deleteAll();

        // Setup adult client (age > 18)
        adultClientDetail = new GetClientDetailResponse();
        adultClientDetail.setBirthDate(ADULT_BIRTH_DATE);
        adultClientDetail.setForename("John");
        adultClientDetail.setSurname("Doe");
        adultClientDetail.setGender(GetClientDetailResponse.GenderEnum.M);
        adultClientDetail.setPep(false);

        // Setup minor client (age < 18)
        minorClientDetail = new GetClientDetailResponse();
        minorClientDetail.setBirthDate(MINOR_BIRTH_DATE);
        minorClientDetail.setForename("Jane");
        minorClientDetail.setSurname("Doe");
        minorClientDetail.setGender(GetClientDetailResponse.GenderEnum.F);
        minorClientDetail.setPep(false);

        // Setup accounts list
        NationalAccount account = new NationalAccount();
        account.setNumber("123456789");
        account.setBankCode("0800");
        account.setProductId("SB022291");
        accountsWithData = Arrays.asList(account);
    }

    @AfterEach
    void cleanup() {
        eligibilityRepository.deleteAll();
    }

    @Test
    void evaluateEligibility_WhenClientHasAccountsAndIsAdult_ShouldSaveEligibleToDatabase() throws ExternalApiException {
        // Given
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenReturn(accountsWithData);
        when(apiServiceClients.getClientDetail(CLIENT_ID, CORRELATION_ID)).thenReturn(adultClientDetail);

        // When
        GetEligibilityResponse response = eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEligible()).isTrue();
        assertThat(response.getReasons()).isNullOrEmpty();

        // Verify database state
        List<Eligibility> savedEligibilities = eligibilityRepository.findByClientId(CLIENT_ID);
        assertThat(savedEligibilities).hasSize(1);

        Eligibility savedEligibility = savedEligibilities.get(0);
        assertThat(savedEligibility.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(savedEligibility.getCorrelationId()).isEqualTo(CORRELATION_ID);
        assertThat(savedEligibility.getResult()).isEqualTo(Eligibility.EligibilityResultEnum.ELIGIBLE);
        assertThat(savedEligibility.getCheckedAt()).isNotNull();
        assertThat(savedEligibility.getId()).isNotNull();
    }

    @Test
    void evaluateEligibility_WhenClientHasNoAccountsAndIsAdult_ShouldSaveNotEligibleToDatabase() throws ExternalApiException {
        // Given
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenReturn(Collections.emptyList());
        when(apiServiceClients.getClientDetail(CLIENT_ID, CORRELATION_ID)).thenReturn(adultClientDetail);

        // When
        GetEligibilityResponse response = eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEligible()).isFalse();
        assertThat(response.getReasons()).hasSize(1);
        assertThat(response.getReasons()).contains(GetEligibilityResponse.ReasonsEnum.NO_ACCOUNT);

        // Verify database state
        List<Eligibility> savedEligibilities = eligibilityRepository.findByClientId(CLIENT_ID);
        assertThat(savedEligibilities).hasSize(1);

        Eligibility savedEligibility = savedEligibilities.get(0);
        assertThat(savedEligibility.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(savedEligibility.getCorrelationId()).isEqualTo(CORRELATION_ID);
        assertThat(savedEligibility.getResult()).isEqualTo(Eligibility.EligibilityResultEnum.NOT_ELIGIBLE);
        assertThat(savedEligibility.getCheckedAt()).isNotNull();
    }

    @Test
    void evaluateEligibility_WhenClientHasNoAccountsAndIsMinor_ShouldSaveNotEligibleWithBothReasonsToDatabase() throws ExternalApiException {
        // Given
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenReturn(Collections.emptyList());
        when(apiServiceClients.getClientDetail(CLIENT_ID, CORRELATION_ID)).thenReturn(minorClientDetail);

        // When
        GetEligibilityResponse response = eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEligible()).isFalse();
        assertThat(response.getReasons()).hasSize(2);
        assertThat(response.getReasons()).containsExactlyInAnyOrder(
                GetEligibilityResponse.ReasonsEnum.NO_ACCOUNT,
                GetEligibilityResponse.ReasonsEnum.NO_ADULT
        );

        // Verify database state
        List<Eligibility> savedEligibilities = eligibilityRepository.findByClientId(CLIENT_ID);
        assertThat(savedEligibilities).hasSize(1);

        Eligibility savedEligibility = savedEligibilities.get(0);
        assertThat(savedEligibility.getResult()).isEqualTo(Eligibility.EligibilityResultEnum.NOT_ELIGIBLE);
    }

    @Test
    void evaluateEligibility_WhenClientHasAccountsButIsMinor_ShouldSaveNotEligibleToDatabase() throws ExternalApiException {
        // Given
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenReturn(accountsWithData);
        when(apiServiceClients.getClientDetail(CLIENT_ID, CORRELATION_ID)).thenReturn(minorClientDetail);

        // When
        GetEligibilityResponse response = eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEligible()).isFalse();
        assertThat(response.getReasons()).hasSize(1);
        assertThat(response.getReasons()).contains(GetEligibilityResponse.ReasonsEnum.NO_ADULT);

        // Verify database state
        List<Eligibility> savedEligibilities = eligibilityRepository.findByClientId(CLIENT_ID);
        assertThat(savedEligibilities).hasSize(1);

        Eligibility savedEligibility = savedEligibilities.get(0);
        assertThat(savedEligibility.getResult()).isEqualTo(Eligibility.EligibilityResultEnum.NOT_ELIGIBLE);
    }

    @Test
    void evaluateEligibility_WhenExternalApiThrowsException_ShouldSaveErrorToDatabase() throws ExternalApiException {
        // Given
        ExternalApiException externalException = new ExternalApiException("External service unavailable",
                new RuntimeException("Connection timeout"));
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenThrow(externalException);

        // When & Then
        assertThatThrownBy(() -> eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID))
                .isInstanceOf(EligibilityApiException.class)
                .hasMessage("External service unavailable")
                .hasCause(externalException);

        // Verify database state - ERROR should be saved even when exception is thrown
        List<Eligibility> savedEligibilities = eligibilityRepository.findByClientId(CLIENT_ID);
        assertThat(savedEligibilities).hasSize(1);

        Eligibility savedEligibility = savedEligibilities.get(0);
        assertThat(savedEligibility.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(savedEligibility.getCorrelationId()).isEqualTo(CORRELATION_ID);
        assertThat(savedEligibility.getResult()).isEqualTo(Eligibility.EligibilityResultEnum.ERROR);
        assertThat(savedEligibility.getCheckedAt()).isNotNull();
    }

    @Test
    void evaluateEligibility_WhenMultipleCallsForSameClient_ShouldSaveMultipleRecords() throws ExternalApiException {
        // Given
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenReturn(accountsWithData);
        when(apiServiceClients.getClientDetail(CLIENT_ID, CORRELATION_ID)).thenReturn(adultClientDetail);

        // When - call the service twice
        eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID);
        eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID);

        // Then - should have 2 records in database
        List<Eligibility> savedEligibilities = eligibilityRepository.findByClientId(CLIENT_ID);
        assertThat(savedEligibilities).hasSize(2);

        for (Eligibility eligibility : savedEligibilities) {
            assertThat(eligibility.getClientId()).isEqualTo(CLIENT_ID);
            assertThat(eligibility.getCorrelationId()).isEqualTo(CORRELATION_ID);
            assertThat(eligibility.getResult()).isEqualTo(Eligibility.EligibilityResultEnum.ELIGIBLE);
        }
    }

    @Test
    void evaluateEligibility_WhenDifferentClients_ShouldSeparateRecordsInDatabase() throws ExternalApiException {
        // Given
        String client1Id = "1111-11-11-11.11.11.111111";
        String client2Id = "2222-22-22-22.22.22.222222";
        String correlation1 = "corr-1";
        String correlation2 = "corr-2";

        when(apiServiceAccounts.getClientAccounts(client1Id, correlation1)).thenReturn(accountsWithData);
        when(apiServiceClients.getClientDetail(client1Id, correlation1)).thenReturn(adultClientDetail);

        when(apiServiceAccounts.getClientAccounts(client2Id, correlation2)).thenReturn(Collections.emptyList());
        when(apiServiceClients.getClientDetail(client2Id, correlation2)).thenReturn(minorClientDetail);

        // When
        eligibilityService.evaluateEligibility(client1Id, correlation1);
        eligibilityService.evaluateEligibility(client2Id, correlation2);

        // Then
        List<Eligibility> client1Records = eligibilityRepository.findByClientId(client1Id);
        List<Eligibility> client2Records = eligibilityRepository.findByClientId(client2Id);

        assertThat(client1Records).hasSize(1);
        assertThat(client2Records).hasSize(1);

        assertThat(client1Records.get(0).getResult()).isEqualTo(Eligibility.EligibilityResultEnum.ELIGIBLE);
        assertThat(client2Records.get(0).getResult()).isEqualTo(Eligibility.EligibilityResultEnum.NOT_ELIGIBLE);
    }

    @Test
    void evaluateEligibility_WithBoundaryAgeInDatabase_ShouldHandleCorrectly() throws ExternalApiException {
        // Test exactly 18 years old (should be adult)
        String eighteenYearsBirthDate = LocalDate.now().minusYears(18).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        GetClientDetailResponse eighteenYearOldClient = new GetClientDetailResponse();
        eighteenYearOldClient.setBirthDate(eighteenYearsBirthDate);
        eighteenYearOldClient.setForename("Boundary");
        eighteenYearOldClient.setSurname("Test");
        eighteenYearOldClient.setGender(GetClientDetailResponse.GenderEnum.M);
        eighteenYearOldClient.setPep(false);

        // Given
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenReturn(accountsWithData);
        when(apiServiceClients.getClientDetail(CLIENT_ID, CORRELATION_ID)).thenReturn(eighteenYearOldClient);

        // When
        GetEligibilityResponse response = eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID);

        // Then
        assertThat(response.getEligible()).isTrue();

        // Verify in database
        List<Eligibility> savedEligibilities = eligibilityRepository.findByClientId(CLIENT_ID);
        assertThat(savedEligibilities).hasSize(1);
        assertThat(savedEligibilities.get(0).getResult()).isEqualTo(Eligibility.EligibilityResultEnum.ELIGIBLE);
    }

    @Test
    void evaluateEligibility_RepositoryFindByCorrelationId_ShouldWork() throws ExternalApiException {
        // Given
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenReturn(accountsWithData);
        when(apiServiceClients.getClientDetail(CLIENT_ID, CORRELATION_ID)).thenReturn(adultClientDetail);

        // When
        eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID);

        // Then - verify we can find by correlation ID
        List<Eligibility> eligibilitiesByCorrelation = eligibilityRepository.findByCorrelationId(CORRELATION_ID);
        assertThat(eligibilitiesByCorrelation).hasSize(1);
        assertThat(eligibilitiesByCorrelation.get(0).getClientId()).isEqualTo(CLIENT_ID);
        assertThat(eligibilitiesByCorrelation.get(0).getResult()).isEqualTo(Eligibility.EligibilityResultEnum.ELIGIBLE);
    }

    @Test
    void evaluateEligibility_RepositoryFindAllByResult_ShouldWork() throws ExternalApiException {
        // Given - create mix of results
        String eligibleClientId = "eligible-client-id";
        String notEligibleClientId = "not-eligible-client-id";

        when(apiServiceAccounts.getClientAccounts(eligibleClientId, CORRELATION_ID)).thenReturn(accountsWithData);
        when(apiServiceClients.getClientDetail(eligibleClientId, CORRELATION_ID)).thenReturn(adultClientDetail);

        when(apiServiceAccounts.getClientAccounts(notEligibleClientId, CORRELATION_ID)).thenReturn(Collections.emptyList());
        when(apiServiceClients.getClientDetail(notEligibleClientId, CORRELATION_ID)).thenReturn(adultClientDetail);

        // When
        eligibilityService.evaluateEligibility(eligibleClientId, CORRELATION_ID);
        eligibilityService.evaluateEligibility(notEligibleClientId, CORRELATION_ID);

        // Then - verify we can query by result
        List<Eligibility> eligibleResults = eligibilityRepository.findAllByResult(Eligibility.EligibilityResultEnum.ELIGIBLE);
        List<Eligibility> notEligibleResults = eligibilityRepository.findAllByResult(Eligibility.EligibilityResultEnum.NOT_ELIGIBLE);

        assertThat(eligibleResults).hasSize(1);
        assertThat(notEligibleResults).hasSize(1);

        assertThat(eligibleResults.get(0).getClientId()).isEqualTo(eligibleClientId);
        assertThat(notEligibleResults.get(0).getClientId()).isEqualTo(notEligibleClientId);
    }

    //CONFIG
    @TestConfiguration
    static class TestConfig {

        @Bean
        public ApiServiceAccounts apiServiceAccounts() {
            return Mockito.mock(ApiServiceAccounts.class);
        }
        @Bean
        public ApiServiceClients apiServiceClients() {
            return Mockito.mock(ApiServiceClients.class);
        }
    }
}

