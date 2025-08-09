package cz.csas.eligibility.service;

import cz.csas.eligibility.entity.Eligibility;
import cz.csas.eligibility.exceptions.EligibilityApiException;
import cz.csas.eligibility.exceptions.ExternalApiException;
import cz.csas.eligibility.model.Account;
import cz.csas.eligibility.model.GetClientDetailResponse;
import cz.csas.eligibility.model.GetEligibilityResponse;
import cz.csas.eligibility.model.NationalAccount;
import cz.csas.eligibility.repository.EligibilityRepository;
import cz.csas.eligibility.service.impl.EligibilityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EligibilityServiceTest {

    @Mock
    private ApiServiceAccounts apiServiceAccounts;

    @Mock
    private ApiServiceClients apiServiceClients;

    @Mock
    private EligibilityRepository eligibilityRepository;

    @InjectMocks
    private EligibilityServiceImpl eligibilityService;

    private static final String CLIENT_ID = "1234-56-78-90.12.34.567890";
    private static final String CORRELATION_ID = "test-correlation-id";
    private static final String ADULT_BIRTH_DATE = "1990-01-01";
    private static final String MINOR_BIRTH_DATE = "2010-01-01";

    private GetClientDetailResponse adultClientDetail;
    private GetClientDetailResponse minorClientDetail;
    private List<Account> accountsWithData;

    @BeforeEach
    void setUp() {
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

        // Mock repository save method
        when(eligibilityRepository.save(any(Eligibility.class)))
                .thenReturn(Eligibility.builder()
                        .id(1L)
                        .clientId(CLIENT_ID)
                        .correlationId(CORRELATION_ID)
                        .result(Eligibility.EligibilityResultEnum.ELIGIBLE)
                        .build());
    }

    @Test
    void evaluateEligibility_WhenClientHasAccountsAndIsAdult_ShouldReturnEligible() throws EligibilityApiException {
        // Given
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenReturn(accountsWithData);
        when(apiServiceClients.getClientDetail(CLIENT_ID, CORRELATION_ID)).thenReturn(adultClientDetail);

        // When
        GetEligibilityResponse response = eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEligible()).isTrue();
        assertThat(response.getReasons()).isNullOrEmpty();

        // Verify that ELIGIBLE result was saved
        ArgumentCaptor<Eligibility> eligibilityCaptor = ArgumentCaptor.forClass(Eligibility.class);
        verify(eligibilityRepository).save(eligibilityCaptor.capture());

        Eligibility savedEligibility = eligibilityCaptor.getValue();
        assertThat(savedEligibility.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(savedEligibility.getCorrelationId()).isEqualTo(CORRELATION_ID);
        assertThat(savedEligibility.getResult()).isEqualTo(Eligibility.EligibilityResultEnum.ELIGIBLE);
    }

    @Test
    void evaluateEligibility_WhenClientHasNoAccountsAndIsAdult_ShouldReturnNotEligibleWithNoAccountReason() throws EligibilityApiException {
        // Given - empty accounts list
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenReturn(Collections.emptyList());
        when(apiServiceClients.getClientDetail(CLIENT_ID, CORRELATION_ID)).thenReturn(adultClientDetail);

        // When
        GetEligibilityResponse response = eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEligible()).isFalse();
        assertThat(response.getReasons()).hasSize(1);
        assertThat(response.getReasons()).contains(GetEligibilityResponse.ReasonsEnum.NO_ACCOUNT);

        // Verify that NOT_ELIGIBLE result was saved
        // Note: ArgumentCaptor is Mockito tool which allows to capture value of the argument which was handed over to mocked method when calling the method.
        ArgumentCaptor<Eligibility> eligibilityCaptor = ArgumentCaptor.forClass(Eligibility.class);
        verify(eligibilityRepository).save(eligibilityCaptor.capture());

        Eligibility savedEligibility = eligibilityCaptor.getValue();
        assertThat(savedEligibility.getResult()).isEqualTo(Eligibility.EligibilityResultEnum.NOT_ELIGIBLE);
    }

    @Test
    void evaluateEligibility_WhenClientHasNullAccountsAndIsAdult_ShouldReturnNotEligibleWithNoAccountReason() throws EligibilityApiException {
        // Given - null accounts list (which will be converted to empty by ApiServiceAccounts implementation)
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenReturn(Collections.emptyList());
        when(apiServiceClients.getClientDetail(CLIENT_ID, CORRELATION_ID)).thenReturn(adultClientDetail);

        // When
        GetEligibilityResponse response = eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEligible()).isFalse();
        assertThat(response.getReasons()).hasSize(1);
        assertThat(response.getReasons()).contains(GetEligibilityResponse.ReasonsEnum.NO_ACCOUNT);

        // Verify that NOT_ELIGIBLE result was saved
        ArgumentCaptor<Eligibility> eligibilityCaptor = ArgumentCaptor.forClass(Eligibility.class);
        verify(eligibilityRepository).save(eligibilityCaptor.capture());

        Eligibility savedEligibility = eligibilityCaptor.getValue();
        assertThat(savedEligibility.getResult()).isEqualTo(Eligibility.EligibilityResultEnum.NOT_ELIGIBLE);
    }

    @Test
    void evaluateEligibility_WhenClientHasNoAccountsAndIsMinor_ShouldReturnNotEligibleWithBothReasons() throws EligibilityApiException {
        // Given - empty accounts list and minor client
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

        // Verify that NOT_ELIGIBLE result was saved
        ArgumentCaptor<Eligibility> eligibilityCaptor = ArgumentCaptor.forClass(Eligibility.class);
        verify(eligibilityRepository).save(eligibilityCaptor.capture());

        Eligibility savedEligibility = eligibilityCaptor.getValue();
        assertThat(savedEligibility.getResult()).isEqualTo(Eligibility.EligibilityResultEnum.NOT_ELIGIBLE);
    }

    @Test
    void evaluateEligibility_WhenClientHasAccountsButIsMinor_ShouldReturnNotEligibleWithNoAdultReason() throws EligibilityApiException {
        // Given - client has accounts but is minor
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenReturn(accountsWithData);
        when(apiServiceClients.getClientDetail(CLIENT_ID, CORRELATION_ID)).thenReturn(minorClientDetail);

        // When
        GetEligibilityResponse response = eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEligible()).isFalse();
        assertThat(response.getReasons()).hasSize(1);
        assertThat(response.getReasons()).contains(GetEligibilityResponse.ReasonsEnum.NO_ADULT);

        // Verify that NOT_ELIGIBLE result was saved
        ArgumentCaptor<Eligibility> eligibilityCaptor = ArgumentCaptor.forClass(Eligibility.class);
        verify(eligibilityRepository).save(eligibilityCaptor.capture());

        Eligibility savedEligibility = eligibilityCaptor.getValue();
        assertThat(savedEligibility.getResult()).isEqualTo(Eligibility.EligibilityResultEnum.NOT_ELIGIBLE);
    }

    @Test
    void evaluateEligibility_WhenAccountsServiceThrowsExternalApiException_ShouldThrowEligibilityApiException() throws EligibilityApiException {
        // Given
        ExternalApiException externalException = new ExternalApiException("Accounts service error", new RuntimeException("Connection failed"));
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenThrow(externalException);

        // When & Then
        assertThatThrownBy(() -> eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID))
                .isInstanceOf(EligibilityApiException.class)
                .hasMessage("Accounts service error")
                .hasCause(externalException);

        // Verify that ERROR result was saved
        ArgumentCaptor<Eligibility> eligibilityCaptor = ArgumentCaptor.forClass(Eligibility.class);
        verify(eligibilityRepository).save(eligibilityCaptor.capture());

        Eligibility savedEligibility = eligibilityCaptor.getValue();
        assertThat(savedEligibility.getResult()).isEqualTo(Eligibility.EligibilityResultEnum.ERROR);
    }

    @Test
    void evaluateEligibility_WhenClientsServiceThrowsExternalApiException_ShouldThrowEligibilityApiException() throws EligibilityApiException {
        // Given
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenReturn(accountsWithData);
        ExternalApiException externalException = new ExternalApiException("Clients service error", new RuntimeException("Connection failed"));
        when(apiServiceClients.getClientDetail(CLIENT_ID, CORRELATION_ID)).thenThrow(externalException);

        // When & Then
        assertThatThrownBy(() -> eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID))
                .isInstanceOf(EligibilityApiException.class)
                .hasMessage("Clients service error")
                .hasCause(externalException);

        // Verify that ERROR result was saved
        ArgumentCaptor<Eligibility> eligibilityCaptor = ArgumentCaptor.forClass(Eligibility.class);
        verify(eligibilityRepository).save(eligibilityCaptor.capture());

        Eligibility savedEligibility = eligibilityCaptor.getValue();
        assertThat(savedEligibility.getResult()).isEqualTo(Eligibility.EligibilityResultEnum.ERROR);
    }

    @Test
    void evaluateEligibility_WhenAnyOtherExceptionOccurs_ShouldThrowEligibilityApiException() throws EligibilityApiException {
        // Given
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenReturn(accountsWithData);
        RuntimeException runtimeException = new RuntimeException("Unexpected error");
        when(apiServiceClients.getClientDetail(CLIENT_ID, CORRELATION_ID)).thenThrow(runtimeException);

        // When & Then
        assertThatThrownBy(() -> eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID))
                .isInstanceOf(EligibilityApiException.class)
                .hasMessage("Unexpected error")
                .hasCause(runtimeException);

        // Verify that ERROR result was saved
        ArgumentCaptor<Eligibility> eligibilityCaptor = ArgumentCaptor.forClass(Eligibility.class);
        verify(eligibilityRepository).save(eligibilityCaptor.capture());

        Eligibility savedEligibility = eligibilityCaptor.getValue();
        assertThat(savedEligibility.getResult()).isEqualTo(Eligibility.EligibilityResultEnum.ERROR);
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void evaluateEligibility_WhenRepositorySaveThrowsException_ShouldStillThrowEligibilityApiException() throws EligibilityApiException {
        // Given
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenReturn(accountsWithData);
        when(apiServiceClients.getClientDetail(CLIENT_ID, CORRELATION_ID)).thenReturn(adultClientDetail);

        // Firs calling of eligibilityRepository.save() in try block will throw RuntimeException to simulate any exception thrown from try block.
        // Second use of eligibilityRepository.save() in catch block is performed without exception (returns null)
        doThrow(new RuntimeException("Database error"))
                .doReturn(null)
                .when(eligibilityRepository).save(any(Eligibility.class));

        // When & Then
        assertThatThrownBy(() -> eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID))
                .isInstanceOf(EligibilityApiException.class);

        // Checking that save() was called 2 times
        verify(eligibilityRepository, times(2)).save(any(Eligibility.class));
    }


    @Test
    void evaluateEligibility_WithBoundaryAgeValues_ShouldHandleCorrectly() throws EligibilityApiException {
        // Test exactly 18 years old (should be adult)
        String eighteenYearsBirthDate = LocalDate.now().minusYears(18).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        GetClientDetailResponse eighteenYearOldClient = new GetClientDetailResponse();
        eighteenYearOldClient.setBirthDate(eighteenYearsBirthDate);
        eighteenYearOldClient.setForename("Test");
        eighteenYearOldClient.setSurname("User");
        eighteenYearOldClient.setGender(GetClientDetailResponse.GenderEnum.M);
        eighteenYearOldClient.setPep(false);

        // Given
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenReturn(accountsWithData);
        when(apiServiceClients.getClientDetail(CLIENT_ID, CORRELATION_ID)).thenReturn(eighteenYearOldClient);

        // When
        GetEligibilityResponse response = eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEligible()).isTrue();
        assertThat(response.getReasons()).isNullOrEmpty();
    }

    @Test
    void evaluateEligibility_WithAlmostEighteenYearOldClient_ShouldReturnNotEligible() throws EligibilityApiException {
        // Test almost 18 years old
        String almostEighteenBirthDate = LocalDate.now().minusYears(18).plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        GetClientDetailResponse almostEighteenClient = new GetClientDetailResponse();
        almostEighteenClient.setBirthDate(almostEighteenBirthDate);
        almostEighteenClient.setForename("Test");
        almostEighteenClient.setSurname("User");
        almostEighteenClient.setGender(GetClientDetailResponse.GenderEnum.M);
        almostEighteenClient.setPep(false);

        // Given
        when(apiServiceAccounts.getClientAccounts(CLIENT_ID, CORRELATION_ID)).thenReturn(accountsWithData);
        when(apiServiceClients.getClientDetail(CLIENT_ID, CORRELATION_ID)).thenReturn(almostEighteenClient);

        // When
        GetEligibilityResponse response = eligibilityService.evaluateEligibility(CLIENT_ID, CORRELATION_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEligible()).isFalse();
        assertThat(response.getReasons()).hasSize(1);
        assertThat(response.getReasons()).contains(GetEligibilityResponse.ReasonsEnum.NO_ADULT);
    }
}
