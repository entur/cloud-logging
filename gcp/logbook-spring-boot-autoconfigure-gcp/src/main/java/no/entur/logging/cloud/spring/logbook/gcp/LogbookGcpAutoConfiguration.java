package no.entur.logging.cloud.spring.logbook.gcp;

import no.entur.logging.cloud.spring.logbook.LogbookLoggingAutoConfiguration;
import no.entur.logging.cloud.spring.logbook.LogbookLoggingCloudProperties;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureBefore(LogbookLoggingAutoConfiguration.class)
public class LogbookGcpAutoConfiguration {

    @Bean
    public LogbookLoggingCloudProperties logbookCloudConfiguration() {
        LogbookLoggingCloudProperties c = new LogbookLoggingCloudProperties();
        // subtract a few kb for headers and other wrapping
        c.setMaxBodySize(131072 - 2 * 1024);
        c.setMaxSize(131072);
        return c;
    }
}