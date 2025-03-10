package no.entur.logging.cloud.gcp.spring.ondemand;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Bean
    @ConditionalOnDisabledOndemandLogging
    public String disabled() {
        return "disabled";
    }

    @Bean
    @ConditionalOnEnabledOndemandLogging
    public String enabled() {
        return "enabled";
    }
}