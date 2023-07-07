package no.entur.logging.cloud.gcp.spring.logbook.webflux;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:logbook.gcp.properties", ignoreResourceNotFound = false)
public class LogbookGcpWebfluxAutoConfiguration {

}
