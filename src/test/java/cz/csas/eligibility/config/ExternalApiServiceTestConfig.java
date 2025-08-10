package cz.csas.eligibility.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@TestConfiguration
public class ExternalApiServiceTestConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        RestTemplate rt = new RestTemplate(
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory())
        );
        return rt;
    }

    @Bean
    public MockRestServiceServer mockServer(RestTemplate restTemplate) {
        return MockRestServiceServer.createServer(restTemplate);
    }
}
