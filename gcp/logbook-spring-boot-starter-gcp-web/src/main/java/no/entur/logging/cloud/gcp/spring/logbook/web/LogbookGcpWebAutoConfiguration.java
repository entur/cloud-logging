package no.entur.logging.cloud.gcp.spring.logbook.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:logbook.gcp.properties", ignoreResourceNotFound = false)
public class LogbookGcpWebAutoConfiguration {

}
