package no.entur.logging.cloud.spring.stderr.micrometer;

import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfiguration {

    @Bean
    public LogbackMetrics logbackMetrics() {
        return new no.entur.logging.cloud.micrometer.DevOpsLogbackMetrics();
    }
}
