package no.entur.logging.cloud.spring.stderr.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot auto-configuration that intercepts {@code System.err} output and increments
 * Micrometer error counters on each newline.
 *
 * <p>Activated by default when a {@link MeterRegistry} bean is present; can be disabled by
 * setting {@code entur.logging.stderr.micrometer.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "entur.logging.stderr.micrometer.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(MeterRegistry.class)
@EnableConfigurationProperties(StderrMicrometerProperties.class)
public class StderrMicrometerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SystemErrMicrometerPrintStream.class)
    public SystemErrMicrometerPrintStream systemErrMicrometerPrintStream(MeterRegistry registry) {
        SystemErrMicrometerPrintStream printStream = new SystemErrMicrometerPrintStream(registry, System.err);
        System.setErr(printStream);
        return printStream;
    }
}
