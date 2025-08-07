package cz.csas.eligibility.service;

import cz.csas.eligibility.exceptions.ExternalApiException;
import cz.csas.eligibility.model.Account;

import java.util.List;

/**
 * Service interface for fetching client accounts from the external Accounts API.
 */
public interface ApiServiceAccounts {
    /**
     * Fetches the list of accounts for the given client ID.
     * @param clientId      the client identifier
     * @param correlationId the correlation id for tracing
     * @return non-nullable List<Account> with accounts; may be empty if client has no accounts
     * @throws ExternalApiException on API communication or server errors
     */
    List<Account> getClientAccounts(String clientId, String correlationId) throws ExternalApiException;
}