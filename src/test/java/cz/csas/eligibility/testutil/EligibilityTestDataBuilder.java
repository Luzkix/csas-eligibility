package cz.csas.eligibility.testutil;

import cz.csas.eligibility.model.Account;
import cz.csas.eligibility.model.GetClientDetailResponse;
import cz.csas.eligibility.model.InternationalAccount;
import cz.csas.eligibility.model.NationalAccount;
import cz.csas.eligibility.model.Currency;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for creating test data objects used in eligibility tests.
 */
public class EligibilityTestDataBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Creates an adult client detail response (age > 18).
     */
    public static GetClientDetailResponse createAdultClient(String birthDate) {
        GetClientDetailResponse client = new GetClientDetailResponse();
        client.setBirthDate(birthDate);
        client.setForename("John");
        client.setSurname("Doe");
        client.setGender(GetClientDetailResponse.GenderEnum.M);
        client.setPep(false);
        client.setClientVerificationLevel(3);
        client.setPrimaryEmail("john.doe@example.com");
        client.setPrimaryPhone("420123456789");
        return client;
    }

    /**
     * Creates a minor client detail response (age < 18).
     */
    public static GetClientDetailResponse createMinorClient(String birthDate) {
        GetClientDetailResponse client = new GetClientDetailResponse();
        client.setBirthDate(birthDate);
        client.setForename("Jane");
        client.setSurname("Smith");
        client.setGender(GetClientDetailResponse.GenderEnum.F);
        client.setPep(false);
        client.setClientVerificationLevel(2);
        client.setPrimaryEmail("jane.smith@example.com");
        return client;
    }

    /**
     * Creates an adult client with exactly 18 years of age.
     */
    public static GetClientDetailResponse createExactly18YearsOldClient() {
        String birthDate = LocalDate.now().minusYears(18).format(DATE_FORMATTER);
        return createAdultClient(birthDate);
    }

    /**
     * Creates a client who is almost 18 (17 years and 364 days old).
     */
    public static GetClientDetailResponse createAlmost18YearsOldClient() {
        String birthDate = LocalDate.now().minusYears(18).plusDays(1).format(DATE_FORMATTER);
        return createMinorClient(birthDate);
    }

    /**
     * Creates a default adult client with birth date "1990-01-01".
     */
    public static GetClientDetailResponse createDefaultAdultClient() {
        return createAdultClient("1990-01-01");
    }

    /**
     * Creates a default minor client with birth date "2010-01-01".
     */
    public static GetClientDetailResponse createDefaultMinorClient() {
        return createMinorClient("2010-01-01");
    }

    /**
     * Creates a list with one national account.
     */
    public static List<Account> createAccountsWithNationalAccount() {
        NationalAccount account = new NationalAccount();
        account.setPrefix("19");
        account.setNumber("123456789");
        account.setBankCode("0800");
        account.setProductId("SB022291");
        account.setClosingDate(null); // Active account
        return Arrays.asList(account);
    }

    /**
     * Creates a list with one international account.
     */
    public static List<Account> createAccountsWithInternationalAccount() {
        InternationalAccount account = new InternationalAccount();
        account.setIban("CZ3908000000000735147003");
        account.setCurrency(Currency.CZK);
        account.setProductId("SB022292");
        account.setClosingDate(null); // Active account
        return Arrays.asList(account);
    }

    /**
     * Creates a list with both national and international accounts.
     */
    public static List<Account> createAccountsWithMultipleAccounts() {
        NationalAccount nationalAccount = new NationalAccount();
        nationalAccount.setPrefix("19");
        nationalAccount.setNumber("111111111");
        nationalAccount.setBankCode("0800");
        nationalAccount.setProductId("SB022291");

        InternationalAccount internationalAccount = new InternationalAccount();
        internationalAccount.setIban("CZ3908000000000735147003");
        internationalAccount.setCurrency(Currency.EUR);
        internationalAccount.setProductId("SB022292");

        return Arrays.asList(nationalAccount, internationalAccount);
    }

    /**
     * Creates an empty accounts list.
     */
    public static List<Account> createEmptyAccountsList() {
        return Collections.emptyList();
    }

    /**
     * Creates a list with a closed account.
     */
    public static List<Account> createAccountsWithClosedAccount() {
        NationalAccount account = new NationalAccount();
        account.setPrefix("19");
        account.setNumber("987654321");
        account.setBankCode("0800");
        account.setProductId("SB022291");
        account.setClosingDate("2023-12-31"); // Closed account
        return Arrays.asList(account);
    }

    /**
     * Creates client IDs for testing.
     */
    public static class ClientIds {
        public static final String VALID_CLIENT_ID = "1234-56-78-90.12.34.567890";
        public static final String ELIGIBLE_CLIENT_ID = "1111-11-11-11.11.11.111111";
        public static final String NOT_ELIGIBLE_CLIENT_ID = "2222-22-22-22.22.22.222222";
        public static final String MINOR_CLIENT_ID = "3333-33-33-33.33.33.333333";
        public static final String ERROR_CLIENT_ID = "9999-99-99-99.99.99.999999";
    }

    /**
     * Creates correlation IDs for testing.
     */
    public static class CorrelationIds {
        public static final String VALID_CORRELATION_ID = "test-correlation-id";
        public static final String INTEGRATION_TEST_CORRELATION_ID = "integration-test-correlation-id";
        public static final String UNIT_TEST_CORRELATION_ID = "unit-test-correlation-id";
        public static final String ERROR_CORRELATION_ID = "error-test-correlation-id";
    }

    /**
     * Creates standard birth dates for testing.
     */
    public static class BirthDates {
        public static final String ADULT_BIRTH_DATE = "1990-01-01";
        public static final String MINOR_BIRTH_DATE = "2010-01-01";
        public static final String ELDERLY_BIRTH_DATE = "1950-05-15";
        public static final String YOUNG_ADULT_BIRTH_DATE = "2000-03-20";
    }

    private EligibilityTestDataBuilder() {
        // Utility class - no instances
    }
}
