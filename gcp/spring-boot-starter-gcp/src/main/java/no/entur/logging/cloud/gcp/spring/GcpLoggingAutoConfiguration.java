package no.entur.logging.cloud.gcp.spring;


import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import no.entur.logging.cloud.gcp.micrometer.StackdriverLogbackMetrics;
import no.entur.logging.cloud.micrometer.DevOpsLogbackMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpLoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(StackdriverLogbackMetrics.class)
    public LogbackMetrics stackdriverLogbackMetrics() {
        return new StackdriverLogbackMetrics();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(DevOpsLogbackMetrics.class)
    public LogbackMetrics logbackMetrics() {
        return new DevOpsLogbackMetrics();
    }

}
