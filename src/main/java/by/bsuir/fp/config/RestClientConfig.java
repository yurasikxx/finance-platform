package by.bsuir.fp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient nbrbRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.nbrb.by")
                .build();
    }
}