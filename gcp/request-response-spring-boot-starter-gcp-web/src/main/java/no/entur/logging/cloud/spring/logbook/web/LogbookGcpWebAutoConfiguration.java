package no.entur.logging.cloud.spring.logbook.web;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration()
@AutoConfigureBefore(LogbookWebAutoConfiguration.class)
@PropertySource(value = "classpath:logbook.gcp.web.properties", ignoreResourceNotFound = false)
public class LogbookGcpWebAutoConfiguration {

}