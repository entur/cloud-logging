package no.entur.logging.cloud.azure.logbook.spring;

import no.entur.logging.cloud.spring.logbook.LogbookLoggingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@AutoConfigureBefore(LogbookLoggingAutoConfiguration.class)
@PropertySource(value = "classpath:logbook.azure.properties", ignoreResourceNotFound = false)
public class LogbookAzureAutoConfiguration {

}