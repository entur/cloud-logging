package no.entur.logging.cloud.azure.spring.test;

import jakarta.annotation.PostConstruct;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfiguration
@ConditionalOnProperty(name = {"entur.logging.style"})
public class LoggingStyleAutoConfiguration {

    @Value("${entur.logging.style}") CompositeConsoleOutputType style;

    @PostConstruct
    public void enforceStyle() {
        CompositeConsoleOutputControl.setOutput(style);
    }

}