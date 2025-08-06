package cz.csas.eligibility.service;

import cz.csas.eligibility.exceptions.ExternalApiException;
import cz.csas.eligibility.model.GetClientDetailResponse;

/**
 * Service interface for fetching client details from the external Clients API.
 */
public interface ApiServiceClients {
    GetClientDetailResponse getClientDetail(String clientId, String correlationId) throws ExternalApiException;
}
