package no.entur.logging.cloud.gcp.spring.test;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import no.entur.logging.cloud.logback.logstash.test.DeduplicatingAppender;

public class DeduplicatingAppenderTest {

    private static class Collector extends AppenderBase<ILoggingEvent> {
        final List<String> lines = new ArrayList<>();

        @Override
        protected void append(ILoggingEvent event) {
            lines.add(event.getLevel() + " " + event.getFormattedMessage());
        }
    }

    @Test
    public void repeatedErrorsAreCollapsedAndCountedOnStop() {
        LoggerContext context = new LoggerContext();
        Collector collector = new Collector();
        collector.setContext(context);
        collector.start();
        DeduplicatingAppender dedup = new DeduplicatingAppender();
        dedup.setContext(context);
        dedup.addAppender(collector);
        dedup.start();
        Logger logger = context.getLogger("test");

        for (int i = 0; i < 5; i++) {
            dedup.doAppend(new ch.qos.logback.classic.spi.LoggingEvent(Logger.class.getName(), logger, Level.ERROR, "failed", new IllegalStateException("x", new NumberFormatException("C")), null));
            dedup.doAppend(new ch.qos.logback.classic.spi.LoggingEvent(Logger.class.getName(), logger, Level.INFO, "info", null, null));
        }
        dedup.doAppend(new ch.qos.logback.classic.spi.LoggingEvent(Logger.class.getName(), logger, Level.WARN, "other", null, null));
        dedup.stop();

        assertThat(collector.lines.stream().filter(l -> l.equals("INFO info")).count()).isEqualTo(5);
        assertThat(collector.lines.stream().filter(l -> l.equals("ERROR failed")).count()).isEqualTo(1);
        assertThat(collector.lines.stream().filter(l -> l.equals("WARN other")).count()).isEqualTo(1);
        assertThat(collector.lines.stream().anyMatch(l -> l.startsWith("ERROR repeated 4 more times (first #") && l.endsWith("failed | java.lang.NumberFormatException: C"))).isTrue();
    }

    @Test
    public void evictedEntriesWriteTheirSummary() {
        LoggerContext context = new LoggerContext();
        Collector collector = new Collector();
        collector.setContext(context);
        collector.start();
        DeduplicatingAppender dedup = new DeduplicatingAppender();
        dedup.setContext(context);
        dedup.setMaxEntries(2);
        dedup.addAppender(collector);
        dedup.start();
        Logger logger = context.getLogger("test");

        for (String message : new String[] { "a", "a", "a", "b", "c" }) {
            dedup.doAppend(new ch.qos.logback.classic.spi.LoggingEvent(Logger.class.getName(), logger, Level.WARN, message, null, null));
        }

        assertThat(collector.lines.stream().anyMatch(l -> l.startsWith("WARN repeated 2 more times") && l.endsWith(": a"))).isTrue();
    }
}
