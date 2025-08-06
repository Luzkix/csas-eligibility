package cz.csas.eligibility.config;

import cz.csas.eligibility.api_accounts.api.AccountsServerApi;
import cz.csas.eligibility.api_clients.api.ClientsServerApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ExternalApiConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
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
