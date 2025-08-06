package cz.csas.eligibility.config;

import cz.csas.eligibility.api_accounts.api.AccountsServerApi;
import cz.csas.eligibility.api_clients.api.ClientsServerApi;
import cz.csas.eligibility.interceptor.AuditLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class ExternalApiConfig {

    private final AuditLoggingInterceptor auditLoggingInterceptor;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Buffering factory umožní číst response body vícekrát
        restTemplate.setRequestFactory(
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory())
        );

        // Přidání audit interceptoru
        restTemplate.getInterceptors().add(auditLoggingInterceptor);

        return restTemplate;
    }

    @Bean
    @Primary
    public cz.csas.eligibility.api_clients.ApiClient clientsApiClient(RestTemplate restTemplate) {
        return new cz.csas.eligibility.api_clients.ApiClient(restTemplate);
    }

    @Bean
    public ClientsServerApi clientsServerApi(cz.csas.eligibility.api_clients.ApiClient clientsApiClient) {
        return new ClientsServerApi(clientsApiClient);
    }

    @Bean
    @Primary
    public cz.csas.eligibility.api_accounts.ApiClient accountsApiClient(RestTemplate restTemplate) {
        return new cz.csas.eligibility.api_accounts.ApiClient(restTemplate);
    }

    @Bean
    public AccountsServerApi accountsServerApi(cz.csas.eligibility.api_accounts.ApiClient accountsApiClient) {
        return new AccountsServerApi(accountsApiClient);
    }
}
