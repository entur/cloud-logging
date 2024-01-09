package no.entur.logging.cloud.gcp.spring.web;


import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import no.entur.logging.cloud.gcp.micrometer.StackdriverLogbackMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpLoggingAutoConfiguration {

    private static final DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(GcpLoggingAutoConfiguration.class);

    @Bean
    @ConditionalOnClass(StackdriverLogbackMetrics.class)
    public LogbackMetrics stackdriverLogbackMetrics() {
        return new StackdriverLogbackMetrics();
    }

    @Bean
    @ConditionalOnClass(no.entur.logging.cloud.micrometer.DevOpsLogbackMetrics.class)
    public LogbackMetrics logbackMetrics() {
        return new no.entur.logging.cloud.micrometer.DevOpsLogbackMetrics();
    }

}
