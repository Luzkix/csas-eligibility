package cz.csas.eligibility.service;

import cz.csas.eligibility.exceptions.ExternalApiException;
import cz.csas.eligibility.model.Account;

import java.util.List;

/**
 * Service interface for fetching client accounts from the external Accounts API.
 */
public interface ApiServiceAccounts {
    List<Account> getClientAccounts(String clientId, String correlationId) throws ExternalApiException;
}