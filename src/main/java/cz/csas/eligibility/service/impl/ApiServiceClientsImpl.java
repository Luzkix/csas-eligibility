package cz.csas.eligibility.service.impl;

import cz.csas.eligibility.api_clients.api.ClientsServerApi;
import cz.csas.eligibility.exceptions.ExternalApiException;
import cz.csas.eligibility.model.GetClientDetailResponse;
import cz.csas.eligibility.service.ApiServiceClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Implementation of ApiServiceClients that calls the external Clients server API.
 */
@Service
public class ApiServiceClientsImpl implements ApiServiceClients {
    private final ClientsServerApi clientsServerApi;
    private final String clientsApiKey;

    public ApiServiceClientsImpl(
            ClientsServerApi clientsServerApi,
            //accountsApiKey is defined in application.properties
            @Value("${apiKeyClientsServer}") String clientsApiKey) {
        this.clientsServerApi = clientsServerApi;
        this.clientsApiKey = clientsApiKey;
    }

    @Override
    public GetClientDetailResponse getClientDetail(String clientId, String correlationId) throws ExternalApiException {
        try {
            // Invoke external API
            return clientsServerApi.clientIdGet(
                    clientId,
                    clientsApiKey,
                    correlationId
            );

        } catch (HttpClientErrorException e) {
            // 4xx errors
            throw new ExternalApiException(
                    "Clients server error when calling Clients API: " + e.getStatusCode(),
                    e
            );
        } catch (RestClientException e) {
            // I/O or 5xx errors
            throw new ExternalApiException(
                    "Internal error when calling Clients API: " + e.getMessage(),
                    e
            );
        }
    }

}
