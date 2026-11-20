package no.entur.logging.cloud.logback.logstash.test.ai.compact;

import static com.google.common.truth.Truth.assertThat;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.spi.FilterReply;

public class CompactFileTest {

    @AfterEach
    public void cleanUp() {
        System.clearProperty(CompactSettings.AI_QUIET_LOGGERS_PROPERTY);
    }

    @Test
    public void loggerIsShortenedToSimpleClassName() {
        assertThat(CompactLoggerConverter.shorten("org.springframework.web.servlet.DispatcherServlet")).isEqualTo("DispatcherServlet");
        assertThat(CompactLoggerConverter.shorten("a.b.Outer$Inner")).isEqualTo("Outer$Inner");
        assertThat(CompactLoggerConverter.shorten("no.entur.logging.cloud")).isEqualTo("no.entur.logging.cloud");
        assertThat(CompactLoggerConverter.shorten("ROOT")).isEqualTo("ROOT");
    }

    @Test
    public void quietLoggersAreFilteredBelowWarn() {
        LoggerContext context = new LoggerContext();
        QuietLoggersFilter filter = new QuietLoggersFilter();
        filter.start();

        assertThat(filter.decide(event(context, "org.apache.catalina.core.StandardService", Level.INFO))).isEqualTo(FilterReply.DENY);
        assertThat(filter.decide(event(context, "org.apache.catalina.core.StandardService", Level.WARN))).isEqualTo(FilterReply.NEUTRAL);
        assertThat(filter.decide(event(context, "org.apache.catalinax.Other", Level.INFO))).isEqualTo(FilterReply.NEUTRAL);
        assertThat(filter.decide(event(context,
                "org.springframework.security.config.annotation.authentication.configuration.InitializeUserDetailsBeanManagerConfigurer$Inner",
                Level.INFO))).isEqualTo(FilterReply.DENY);
        assertThat(filter.decide(event(context, "com.example.App", Level.INFO))).isEqualTo(FilterReply.NEUTRAL);
        assertThat(filter.decide(event(context, "org.springframework.boot.tomcat.TomcatWebServer", Level.INFO,
                "Tomcat started on port 38013 (http) with context path '/'"))).isEqualTo(FilterReply.NEUTRAL);
        assertThat(filter.decide(event(context, "org.springframework.boot.tomcat.TomcatWebServer", Level.INFO,
                "Tomcat initialized with port 0 (http)"))).isEqualTo(FilterReply.DENY);
        assertThat(filter.decide(event(context, "org.springframework.grpc.server.lifecycle.GrpcServerLifecycle", Level.INFO,
                "gRPC Server started, listening on address: [/[0:0:0:0:0:0:0:0]:9090], port: 9090"))).isEqualTo(FilterReply.NEUTRAL);

        System.setProperty(CompactSettings.AI_QUIET_LOGGERS_PROPERTY, "");
        QuietLoggersFilter none = new QuietLoggersFilter();
        none.start();
        assertThat(none.decide(event(context, "org.apache.catalina.core.StandardService", Level.INFO))).isEqualTo(FilterReply.NEUTRAL);
    }

    private static LoggingEvent event(LoggerContext context, String logger, Level level) {
        return event(context, logger, level, "message");
    }

    private static LoggingEvent event(LoggerContext context, String logger, Level level, String message) {
        LoggingEvent event = new LoggingEvent("f", context.getLogger(logger), level, message, null, null);
        event.setMDCPropertyMap(Map.of());
        return event;
    }
}
