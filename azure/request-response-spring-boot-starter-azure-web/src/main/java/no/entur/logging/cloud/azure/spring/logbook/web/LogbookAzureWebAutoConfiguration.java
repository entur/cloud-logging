package no.entur.logging.cloud.azure.spring.logbook.web;

import no.entur.logging.cloud.spring.logbook.web.LogbookWebAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import static jakarta.servlet.DispatcherType.ERROR;
import static jakarta.servlet.DispatcherType.REQUEST;

@Configuration
@AutoConfigureBefore(LogbookWebAutoConfiguration.class)
@PropertySource(value = "classpath:logbook.azure.web.properties", ignoreResourceNotFound = false)
public class LogbookAzureWebAutoConfiguration {

}