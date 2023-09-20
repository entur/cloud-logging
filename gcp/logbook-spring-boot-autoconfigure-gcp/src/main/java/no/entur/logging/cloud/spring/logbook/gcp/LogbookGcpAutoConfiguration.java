package no.entur.logging.cloud.spring.logbook.gcp;

import no.entur.logging.cloud.spring.logbook.LogbookLoggingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@AutoConfigureBefore(LogbookLoggingAutoConfiguration.class)
@PropertySource(value = "classpath:logbook.gcp.properties", ignoreResourceNotFound = false)
public class LogbookGcpAutoConfiguration {

}