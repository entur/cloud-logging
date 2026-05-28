package no.entur.logging.cloud.spring.stderr;

import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.io.PrintStream;
import java.util.Locale;

/**
 * Spring Boot auto-configuration that redirects {@code System.err} output to SLF4J.
 *
 * <p>Activated by default; can be disabled by setting
 * {@code entur.logging.stderr.enabled=false}.
 *
 * <p>The SLF4J log level and logger name are configurable via
 * {@code entur.logging.stderr.level} and {@code entur.logging.stderr.logger}.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "entur.logging.stderr.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(StderrLoggingProperties.class)
public class StderrLoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SystemErrToSlf4jPrintStream.class)
    public SystemErrToSlf4jPrintStream systemErrToSlf4jPrintStream(StderrLoggingProperties properties) {
        org.slf4j.Logger logger = LoggerFactory.getLogger(properties.getLogger());
        Level level;
        try {
            level = Level.valueOf(properties.getLevel().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid stderr log level '" + properties.getLevel() + "'. " +
                    "Valid values are: trace, debug, info, warn, error", e);
        }

        PrintStream original = System.err;
        SystemErrToSlf4jPrintStream printStream = new SystemErrToSlf4jPrintStream(logger, level, original);
        System.setErr(printStream);
        return printStream;
    }
}
