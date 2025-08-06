package cz.csas.eligibility.service.impl;

import cz.csas.eligibility.api_accounts.api.AccountsServerApi;
import cz.csas.eligibility.exceptions.ExternalApiException;
import cz.csas.eligibility.model.Account;
import cz.csas.eligibility.model.GetAccountsRequest;
import cz.csas.eligibility.model.GetAccountsResponse;
import cz.csas.eligibility.service.ApiServiceAccounts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of ApiServiceAccounts that calls the external Accounts server API.
 */
@Service
public class ApiServiceAccountsImpl implements ApiServiceAccounts {

    private final AccountsServerApi accountsServerApi;
    private final String accountsApiKey;

    public ApiServiceAccountsImpl(
            AccountsServerApi accountsServerApi,
            //accountsApiKey is defined in application.properties
            @Value("${apiKeyAccountsServer}") String accountsApiKey) {
        this.accountsServerApi = accountsServerApi;
        this.accountsApiKey = accountsApiKey;
    }

    /**
     * Fetches the list of accounts for the given client ID and correlation ID.
     * @param clientId      the client identifier (required)
     * @param correlationId the correlation id for tracing (optional)
     * @return list of Account; may be empty if client has no accounts
     * @throws ExternalApiException on communication or server errors
     */
    @Override
    public List<Account> getClientAccounts(String clientId, String correlationId) throws ExternalApiException {
        // Build the request payload
        GetAccountsRequest request = new GetAccountsRequest(clientId);

        try {
            // Invoke external API
            GetAccountsResponse response = accountsServerApi.listGet(
                    clientId,
                    accountsApiKey,
                    correlationId,
                    request
            );

            // Extract and return the list of accounts. In case of null return empty List
            return Objects.requireNonNullElse(response.getAccounts(), Collections.emptyList());

        } catch (HttpClientErrorException e) {
            // 4xx errors
            throw new ExternalApiException(
                    "Accounts server error when calling Accounts API: " + e.getStatusCode(),
                    e
            );
        } catch (RestClientException e) {
            // I/O or 5xx errors
            throw new ExternalApiException(
                    "Internal error when calling Accounts API: " + e.getMessage(),
                    e
            );
        }
    }
}
