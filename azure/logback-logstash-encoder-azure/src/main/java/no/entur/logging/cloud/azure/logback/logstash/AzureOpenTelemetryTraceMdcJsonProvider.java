package no.entur.logging.cloud.azure.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.composite.AbstractJsonProvider;
import tools.jackson.core.JsonGenerator;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

/**
 * An MDC provider that maps OpenTelemetry trace fields to the special JSON fields
 * recognized by Azure: https://docs.azure.cn/en-us/spring-apps/basic-standard/structured-app-log
 *
 */
public class AzureOpenTelemetryTraceMdcJsonProvider extends AbstractJsonProvider<ILoggingEvent> {

    public static final String OPENTELEMETRY_TRACE_ID_KEY = "trace_id";
    public static final String OPENTELEMETRY_SPAN_ID_KEY = "span_id";

    public static final String AZURE_TRACE_KEY = "traceId";
    public static final String AZURE_SPAN_ID_KEY = "spanId";

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) {
        Map<String, String> mdcProperties = event.getMDCPropertyMap();
        if (mdcProperties == null || mdcProperties.isEmpty()) {
            return;
        }

        // map OTel MDC keys to GCP special fields; write all others as-is.
        for (Map.Entry<String, String> entry : mdcProperties.entrySet()) {
            String key = entry.getKey();
            if (key == null) continue;
            String value = entry.getValue();
            if (value == null) continue;

            switch (key) {
                case OPENTELEMETRY_TRACE_ID_KEY -> generator.writeStringProperty(AZURE_TRACE_KEY, value);
                case OPENTELEMETRY_SPAN_ID_KEY  -> generator.writeStringProperty(AZURE_SPAN_ID_KEY, value);
                default -> generator.writeStringProperty(key, value);
            }
        }
    }

    public static boolean isOtelAgent() {
        // 1. Check direct JVM command-line arguments (-javaagent)
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : jvmArgs) {
            if (isOtelArgument(arg)) {
                return true;
            }
        }

        // 2. Backup check for environment variables that inject JVM arguments
        String javaToolOptions = System.getenv("JAVA_TOOL_OPTIONS");
        if (javaToolOptions != null && isOtelArgument(javaToolOptions)) {
            return true;
        }

        return false;
    }

    private static boolean isOtelArgument(String argument) {
        String lowerArg = argument.toLowerCase();
        return lowerArg.contains("-javaagent:") && lowerArg.contains("opentelemetry");
    }

}
