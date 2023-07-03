package no.entur.logging.cloud.gcp.spring.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:logback/logging.test.properties", ignoreResourceNotFound = false)
public class LoggingAutoConfiguration {

}
