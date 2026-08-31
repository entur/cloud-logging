package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.LogstashFormatter;
import net.logstash.logback.composite.AbstractCompositeJsonFormatter;
import net.logstash.logback.composite.JsonProvider;
import net.logstash.logback.composite.loggingevent.*;
import net.logstash.logback.encoder.LogstashEncoder;

import java.util.ArrayList;
import java.util.List;

/**
 * An encoder which add stacktraces to the message field, if present. Also logs log level as severity, which is picked
 * up by the Stackdriver fluentd wrapper.
 * Intended for structured JSON-logging to console 
 * where the logged contents is a jsonPayload.
 * 
 * @see <a href="https://cloud.google.com/error-reporting/docs/formatting-error-messages">formatting-error-messages</a>
 * @see <a href="https://github.com/ankurcha/gcloud-logging-slf4j-logback">gcloud-logging-slf4j-logback</a>
 */

public class StackdriverLogstashEncoder extends LogstashEncoder {

	@Override
	protected AbstractCompositeJsonFormatter<ILoggingEvent> createFormatter() {
		LogstashFormatter formatter = (LogstashFormatter) super.createFormatter();

		LoggingEventJsonProviders loggingEventJsonProviders = formatter.getProviders();
		List<JsonProvider<ILoggingEvent>> providers = new ArrayList<>(loggingEventJsonProviders.getProviders());

		for (JsonProvider<ILoggingEvent> jsonProvider : providers) {
			if(jsonProvider instanceof MessageJsonProvider) {
				loggingEventJsonProviders.removeProvider(jsonProvider);
			} else if(jsonProvider instanceof StackTraceJsonProvider) {
				loggingEventJsonProviders.removeProvider(jsonProvider);
			} else if(jsonProvider instanceof LogLevelJsonProvider) {
				loggingEventJsonProviders.removeProvider(jsonProvider);
			} else if(jsonProvider instanceof TagsJsonProvider) {
				// we only want to use json markers, so omit this "tags" element
				// TODO subclass TagsJonProvider to also ignore our log level marker
				loggingEventJsonProviders.removeProvider(jsonProvider);
			} else if(jsonProvider instanceof LogLevelValueJsonProvider) {
				// stackdriver supports the equivalent functionality as the log level value directly in queries
				// see https://cloud.google.com/logging/docs/view/advanced-filters
				loggingEventJsonProviders.removeProvider(jsonProvider);
			} else if(jsonProvider instanceof MdcJsonProvider p) {
				loggingEventJsonProviders.removeProvider(jsonProvider);

				String projectId = resolveProjectId();

				if(StackdriverOpenTelemetryTraceMdcJsonProvider.isOtelAgent()) {
					loggingEventJsonProviders.addProvider(new StackdriverOpenTelemetryTraceMdcJsonProvider(projectId));
				} else {
					loggingEventJsonProviders.addProvider(new StackdriverMicrometerTraceMdcJsonProvider(projectId));
				}
			}
		}

		loggingEventJsonProviders.addProvider(new StackdriverLogSeverityJsonProvider());
		loggingEventJsonProviders.addProvider(new StackdriverMessageJsonProvider(formatter));

		return formatter;
	}

	private static final String OTEL_GCP_PROJECT_ID_ATTRIBUTE = "gcp.project_id";

	/**
	 * Resolves the GCP project that traces are stored in, so it can be embedded in the
	 * 'projects/{projectId}/traces/{traceId}' value written to the log entry.
	 * 
	 * On Kubernetes, traces are exported to the cluster's host project (ent-kub-<env>),
	 * which differs from the workload's own project. That destination project is carried in the
	 * 'gcp.project_id' OpenTelemetry resource attribute, so it takes precedence. Runtimes that
	 * still export traces to their own project (e.g. Cloud Run, Firebase) fall back to
	 * 'GOOGLE_CLOUD_PROJECT'.
	 */
	private static String resolveProjectId() {
		String projectId = resolveOtelResourceAttribute(OTEL_GCP_PROJECT_ID_ATTRIBUTE);
		if (projectId != null) {
			return projectId;
		}
		return System.getenv("GOOGLE_CLOUD_PROJECT");
	}

	private static String resolveOtelResourceAttribute(String attributeName) {
		String otelResourceAttributes = System.getenv("OTEL_RESOURCE_ATTRIBUTES");
		if (otelResourceAttributes == null || otelResourceAttributes.isBlank()) {
			return null;
		}
		for (String pair : otelResourceAttributes.split(",")) {
			int separatorIndex = pair.indexOf('=');
			if (separatorIndex < 0) {
				continue;
			}
			if (pair.substring(0, separatorIndex).trim().equals(attributeName)) {
				String value = pair.substring(separatorIndex + 1).trim();
				return value.isBlank() ? null : value;
			}
		}
		return null;
	}

}
