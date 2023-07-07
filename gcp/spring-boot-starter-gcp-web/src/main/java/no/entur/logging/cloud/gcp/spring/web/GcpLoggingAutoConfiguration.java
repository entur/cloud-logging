package no.entur.logging.cloud.gcp.spring.web;


import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import no.entur.logging.cloud.gcp.micrometer.StackdriverLogbackMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
public class GcpLoggingAutoConfiguration {

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
