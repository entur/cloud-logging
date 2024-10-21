package no.entur.logging.cloud.azure.spring;


import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import no.entur.logging.cloud.azure.micrometer.AzureLogbackMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureLoggingAutoConfiguration {

    private static final DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(AzureLoggingAutoConfiguration.class);

    @Bean
    @ConditionalOnClass(AzureLogbackMetrics.class)
    public LogbackMetrics azureLogbackMetrics() {
        return new AzureLogbackMetrics();
    }

    @Bean
    @ConditionalOnClass(no.entur.logging.cloud.micrometer.DevOpsLogbackMetrics.class)
    public LogbackMetrics logbackMetrics() {
        return new no.entur.logging.cloud.micrometer.DevOpsLogbackMetrics();
    }

}
