package cz.csas.eligibility.service;

import cz.csas.eligibility.exceptions.ExternalApiException;
import cz.csas.eligibility.model.GetClientDetailResponse;

/**
 * Service interface for fetching client details from the external Clients API.
 */
public interface ApiServiceClients {
    /**
     * Fetches client details based on provided clientId.
     * @param clientId      the client identifier
     * @param correlationId the correlation id for tracing
     * @return GetClientDetailResponse with client's details
     * @throws ExternalApiException on API communication or server errors
     */
    GetClientDetailResponse getClientDetail(String clientId, String correlationId) throws ExternalApiException;
}
