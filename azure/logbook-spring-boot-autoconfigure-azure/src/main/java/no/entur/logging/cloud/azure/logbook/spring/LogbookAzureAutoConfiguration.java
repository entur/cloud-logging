package no.entur.logging.cloud.azure.logbook.spring;

import no.entur.logging.cloud.spring.logbook.LogbookLoggingAutoConfiguration;
import no.entur.logging.cloud.spring.logbook.LogbookLoggingCloudProperties;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@AutoConfigureBefore(LogbookLoggingAutoConfiguration.class)
public class LogbookAzureAutoConfiguration {

    @Bean
    public LogbookLoggingCloudProperties logbookCloudConfiguration() {
        LogbookLoggingCloudProperties c = new LogbookLoggingCloudProperties();
        // subtract a few kb for headers and other wrapping
        c.setMaxBodySize(16384 - 2 * 1024);
        c.setMaxSize(16384);
        return c;
    }

}