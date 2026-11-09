package no.entur.logging.cloud.logback.logstash.test;

import static net.logstash.logback.marker.Markers.appendRaw;

import java.io.ByteArrayInputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import net.logstash.logback.marker.LogstashMarker;

/**
 * Microbenchmark (see <code>test/test-logback/ai-benchmark/formatting-benchmark.sh</code>): time and allocation per event for the AI
 * files (the compact and the full line, with the patterns and converters of the GCP <code>logback-ai.xml</code>) against the JSON encoder
 * of the machine-readable output, for four kinds of events. Writing to the files is not included. Only runs if the environment variable
 * <code>AI_FORMATTING_BENCHMARK</code> names the output file.
 */
@EnabledIfEnvironmentVariable(named = "AI_FORMATTING_BENCHMARK", matches = ".+")
public class AiFormattingBenchmark {

    private static final String DEFAULT_CONFIG = "../../gcp/spring-boot-autoconfigure-gcp-test/src/main/resources/logback/logback-ai.xml";

    private static final String UUID_A = UUID.randomUUID().toString();
    private static final String UUID_B = UUID.randomUUID().toString();

    @Test
    public void formatEvents() throws Exception {
        String config = System.getenv().getOrDefault("AI_FORMATTING_BENCHMARK_CONFIG", DEFAULT_CONFIG);
        LoggerContext context = configure(Files.readString(Path.of(config)));
        ch.qos.logback.classic.Logger root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        Encoder<ILoggingEvent> compact = encoder(root, "COMPACT");
        Encoder<ILoggingEvent> full = encoder(root, "FULL");
        Encoder<ILoggingEvent> json = encoder(root, "JSON");

        Map<String, String> mdc = new LinkedHashMap<>();
        mdc.put("correlationId", UUID_A);
        mdc.put("logging.googleapis.com/trace", UUID_A);
        mdc.put("requestId", UUID_B);
        mdc.put("logging.googleapis.com/spanId", "00f067aa0ba902b7");
        String smallBody = body(1_000);
        String bigBody = body(100_000);
        Exception exception = nested();

        Map<String, Supplier<ILoggingEvent>> events = new LinkedHashMap<>();
        events.put("Plain message with MDC", () -> event(context, Level.INFO, "org.entur.example.web.rest.DocumentEndpoint", "Hello entity with secret / info", null, null, mdc));
        events.put("HTTP response, 1 KB JSON body", () -> event(context, Level.INFO, "no.entur.logging.cloud", "200 OK http://localhost:8080/api/document/some/method (in 5 ms)", http(smallBody), null, mdc));
        events.put("HTTP response, 100 KB JSON body", () -> event(context, Level.INFO, "no.entur.logging.cloud", "200 OK http://localhost:8080/api/document/some/big (in 5 ms)", http(bigBody), null, mdc));
        events.put("ERROR with a 3-exception trace", () -> event(context, Level.ERROR, "org.entur.example.web.rest.DocumentEndpoint", "Failed", null, exception, mdc));

        StringBuilder out = new StringBuilder(String.format(Locale.ROOT, "| Event | JSON encoder | AI files (compact + full line) |%n|---|---|---|%n"));
        for (Map.Entry<String, Supplier<ILoggingEvent>> entry : events.entrySet()) {
            int n = entry.getKey().contains("100 KB") ? 2_000 : entry.getKey().contains("ERROR") ? 20_000 : 100_000;
            // warm up both, then measure
            measure(entry.getValue(), json, null, n / 2);
            measure(entry.getValue(), compact, full, n / 2);
            double[] j = measure(entry.getValue(), json, null, n);
            double[] a = measure(entry.getValue(), compact, full, n);
            out.append(String.format(Locale.ROOT, "| %s | %.1f µs, %.1f KB | %.1f µs, %.1f KB |%n", entry.getKey(), j[0], j[1], a[0], a[1]));
        }
        Files.writeString(Path.of(System.getenv("AI_FORMATTING_BENCHMARK")), out.toString());
    }

    /** The conversion rules and the compact and full patterns of the AI configuration, each in an appender which writes nothing. */
    private static LoggerContext configure(String aiXml) throws Exception {
        StringBuilder rules = new StringBuilder();
        Matcher rule = Pattern.compile("<conversionRule[^>]*/>").matcher(aiXml);
        while (rule.find()) {
            rules.append(rule.group()).append('\n');
        }
        List<String> patterns = new ArrayList<>();
        Matcher pattern = Pattern.compile("<pattern>(.*?)</pattern>").matcher(aiXml);
        while (pattern.find()) {
            patterns.add(pattern.group(1));
        }
        String noop = NoopAppender.class.getName();
        String config = "<configuration>" + rules
                + "<appender name='COMPACT' class='" + noop + "'><encoder class='ch.qos.logback.classic.encoder.PatternLayoutEncoder'><pattern>" + patterns.get(0) + "</pattern></encoder></appender>"
                + "<appender name='FULL' class='" + noop + "'><encoder class='ch.qos.logback.classic.encoder.PatternLayoutEncoder'><pattern>" + patterns.get(1) + "</pattern></encoder></appender>"
                + "<appender name='JSON' class='" + noop + "'><encoder class='net.logstash.logback.encoder.LogstashEncoder'/></appender>"
                + "<root level='DEBUG'><appender-ref ref='COMPACT'/><appender-ref ref='FULL'/><appender-ref ref='JSON'/></root></configuration>";
        LoggerContext context = new LoggerContext();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8)));
        return context;
    }

    @SuppressWarnings("unchecked")
    private static Encoder<ILoggingEvent> encoder(ch.qos.logback.classic.Logger root, String appender) {
        return ((OutputStreamAppender<ILoggingEvent>) root.getAppender(appender)).getEncoder();
    }

    /** Microseconds and kilobytes allocated per event (a new event each time, as each event is parsed once for both lines). */
    private static double[] measure(Supplier<ILoggingEvent> events, Encoder<ILoggingEvent> first, Encoder<ILoggingEvent> second, int n) {
        com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long written = 0;
        long bytes = threads.getCurrentThreadAllocatedBytes();
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            ILoggingEvent event = events.get();
            written += first.encode(event).length;
            if (second != null) {
                written += second.encode(event).length;
            }
        }
        long time = System.nanoTime() - start;
        bytes = threads.getCurrentThreadAllocatedBytes() - bytes;
        if (written == 0) {
            throw new IllegalStateException("nothing written");
        }
        return new double[]{time / 1000.0 / n, bytes / 1024.0 / n};
    }

    private static ILoggingEvent event(LoggerContext context, Level level, String logger, String message, LogstashMarker marker, Throwable throwable,
                                       Map<String, String> mdc) {
        LoggingEvent event = new LoggingEvent("f", context.getLogger(logger), level, message, throwable, null);
        if (marker != null) {
            event.addMarker(marker);
        }
        event.setMDCPropertyMap(mdc);
        return event;
    }

    /** As written by the HTTP request-response sink in AI mode: the request-response marker with the body, and the AI body marker. */
    private static LogstashMarker http(String body) {
        LogstashMarker marker = appendRaw("http", "{\"origin\":\"local\",\"type\":\"response\",\"duration\":5,\"protocol\":\"HTTP/1.1\",\"status\":200,"
                + "\"headers\":{\"content-type\":[\"application/json\"],\"date\":[\"Mon, 09 Nov 2026 19:50:30 GMT\"],\"transfer-encoding\":[\"chunked\"],"
                + "\"x-correlation-id\":[\"" + UUID_A + "\"],\"cache-control\":[\"no-cache\"],\"x-content-type-options\":[\"nosniff\"],"
                + "\"traceparent\":[\"00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01\"]},\"body\":" + body + "}");
        marker.add(new AiBodyMarker(body, true));
        return marker;
    }

    /** A JSON array of small objects with UUIDs, numbers and strings, of about the given size. */
    private static String body(int size) {
        StringBuilder builder = new StringBuilder("{\"items\":[");
        int i = 0;
        while (builder.length() < size) {
            builder.append(i == 0 ? "" : ",").append("{\"id\":\"").append(i % 2 == 0 ? UUID_A : UUID_B).append("\",\"name\":\"item ").append(i)
                    .append("\",\"price\":").append(i * 1.5).append(",\"tags\":[\"a\",\"b\"]}");
            i++;
        }
        return builder.append("]}").toString();
    }

    private static Exception nested() {
        try {
            try {
                try {
                    Integer.parseInt("x");
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("bad input", e);
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("request failed", e);
            }
        } catch (IllegalStateException e) {
            return e;
        }
        return null;
    }
}
