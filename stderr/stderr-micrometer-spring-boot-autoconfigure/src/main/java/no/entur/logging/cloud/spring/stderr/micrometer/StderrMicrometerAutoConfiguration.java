package no.entur.logging.cloud.spring.stderr.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import no.entur.logging.cloud.micrometer.counter.EventCounter;
import no.entur.logging.cloud.micrometer.counter.EventCounterFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Collections;

/**
 * Spring Boot auto-configuration that intercepts {@code System.err} output and increments
 * Micrometer error counters on each newline.
 *
 * <p>Activated by default when a {@link MeterRegistry} bean is present; can be disabled by
 * setting {@code entur.logging.stderr.micrometer.enabled=false}.
 */
@AutoConfiguration(
        afterName = {
                "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
                "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
                "org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration"
        },
        beforeName = "org.springframework.boot.micrometer.metrics.autoconfigure.logging.logback.LogbackMetricsAutoConfiguration"
)
@ConditionalOnProperty(name = "entur.logging.stderr.micrometer.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(StderrMicrometerProperties.class)
public class StderrMicrometerAutoConfiguration {

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean(SystemErrMicrometerPrintStream.class)
    public SystemErrMicrometerPrintStream systemErrMicrometerPrintStream(MeterRegistry registry) {
        EventCounterFactory factory = EventCounterFactory.forCurrentMicrometerVersion();

        EventCounter errorCount = factory.register("logback.events", registry, Collections.emptyList(),
                "level", "error",
                "Number of all error level events that made it to the logs (errorTellMeTomorrow + errorInterruptMyDinner + errorWakeMeUpRightNow)");

        EventCounter errorTellMeTomorrowCount = factory.register("logback.events", registry, Collections.emptyList(),
                "level", "errorTellMeTomorrow",
                "Number of error 'Tell Me Tomorrow' level events that made it to the logs");

        SystemErrMicrometerPrintStream printStream = new SystemErrMicrometerPrintStream(System.err, errorCount, errorTellMeTomorrowCount);
        System.setErr(printStream);
        return printStream;
    }

}
