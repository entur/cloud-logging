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

	public StackdriverLogstashEncoder() {
	}

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

				boolean openTelemetry = detectOpenTelemetry();
				if (openTelemetry) {
					loggingEventJsonProviders.addProvider(new StackdriverOpenTelemetryTraceMdcJsonProvider());
				} else {
					loggingEventJsonProviders.addProvider(new SimpleMdcJsonProvider());
				}
			}
		}

		loggingEventJsonProviders.addProvider(new StackdriverLogSeverityJsonProvider());
		loggingEventJsonProviders.addProvider(new StackdriverMessageJsonProvider(formatter));

		return formatter;
	}

	private boolean detectOpenTelemetry() {
		try {
			Class.forName("io.opentelemetry.api.OpenTelemetry");
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
