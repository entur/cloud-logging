package org.entur.example.web;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.AppenderAttachable;
import no.entur.logging.cloud.gcp.logback.logstash.StackdriverLogstashEncoder;
import no.entur.logging.cloud.gcp.logback.logstash.StackdriverMicrometerTraceMdcJsonProvider;
import no.entur.logging.cloud.gcp.logback.logstash.StackdriverOpenTelemetryTraceMdcJsonProvider;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Iterator;

import static com.google.common.truth.Truth.assertThat;

/**
 * Verifies that the {@link StackdriverLogstashEncoder} selects
 * {@link StackdriverOpenTelemetryTraceMdcJsonProvider} when the OpenTelemetry Java agent is
 * attached (as configured in this module's build.gradle via {@code -javaagent}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProviderSelectionTest {

    @Test
    public void encoderUsesOpenTelemetryTraceMdcJsonProvider() {
        StackdriverLogstashEncoder encoder = findEncoder();
        assertThat(encoder).isNotNull();

        boolean hasOtel = encoder.getProviders().getProviders().stream().anyMatch(p -> p instanceof StackdriverOpenTelemetryTraceMdcJsonProvider);
        boolean hasMicrometer = encoder.getProviders().getProviders().stream().anyMatch(p -> p instanceof StackdriverMicrometerTraceMdcJsonProvider);

        assertThat(hasOtel).isTrue();
        assertThat(hasMicrometer).isFalse();
    }

    private static StackdriverLogstashEncoder findEncoder() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = ctx.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        return searchForEncoder(root);
    }

    @SuppressWarnings("unchecked")
    private static StackdriverLogstashEncoder searchForEncoder(AppenderAttachable<?> attachable) {
        Iterator<Appender<?>> iter = (Iterator) attachable.iteratorForAppenders();
        while (iter.hasNext()) {
            Appender<?> appender = iter.next();
            if (appender instanceof CompositeConsoleAppender<?> composite) {
                // Test appender: the machine-readable encoder is the StackdriverLogstashEncoder
                Encoder<?> enc = composite.getMachineReadableJsonEncoder();
                if (enc instanceof StackdriverLogstashEncoder stackdriverEncoder) {
                    return stackdriverEncoder;
                }
            } else if (appender instanceof ConsoleAppender<?> consoleAppender) {
                if (consoleAppender.getEncoder() instanceof StackdriverLogstashEncoder enc) {
                    return enc;
                }
            }
            if (appender instanceof AppenderAttachable<?> nested) {
                StackdriverLogstashEncoder result = searchForEncoder(nested);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
