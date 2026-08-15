package org.entur.example.web.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestClient restClient(ObservationRegistry observationRegistry) {
        return RestClient.builder()
                .observationRegistry(observationRegistry)
                .build();
    }
}
