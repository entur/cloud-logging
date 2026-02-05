package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.pattern.ExtendedThrowableProxyConverter;
import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import tools.jackson.core.JsonGenerator;
import net.logstash.logback.LogstashFormatter;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;

import java.io.IOException;

public class StackdriverMessageJsonProvider extends MessageJsonProvider {

	private final LogstashFormatter formatter;
	private ThrowableHandlingConverter throwableConverter;

	public StackdriverMessageJsonProvider(LogstashFormatter formatter) {
		this.formatter = formatter;
	}

	@Override
	public void start() {
		super.start();
		throwableConverter = formatter.getThrowableConverter();
		if(throwableConverter == null) {
			// this should never happen
			throwableConverter = createThrowableConverter();
		}

		if(!throwableConverter.isStarted()) {
			throwableConverter.start();
		}
	}

	protected ThrowableHandlingConverter createThrowableConverter() {
		ExtendedThrowableProxyConverter converter = new ExtendedThrowableProxyConverter();
		converter.setContext(getContext());
		return converter;
	}

	@Override
	public void stop() {
		super.stop();
		if(throwableConverter != null) {
			if(throwableConverter.isStarted()) {
				throwableConverter.stop();
			}
		}
	}

	@Override
	public void writeTo(JsonGenerator generator, ILoggingEvent event) {
		IThrowableProxy throwableProxy = event.getThrowableProxy();
		if (throwableProxy != null) {
			String formattedMessage = event.getFormattedMessage();

			String stacktrace = throwableConverter.convert(event);

			boolean writeFormattedMessage = formattedMessage != null && !formattedMessage.isEmpty();
			boolean writeStacktrace = stacktrace != null && !stacktrace.isEmpty();

			if(writeFormattedMessage && writeStacktrace) {
				// stacktrace is on the form:
				// "exception-name colon exception-message newline tab at x.y.z newline tab at a.b.c and so on"
				// so add a space and potentially a dot between the log statement message and the first line of the
				// formatted stacktrace
				String message;
				if (Character.isLetterOrDigit(formattedMessage.charAt(formattedMessage.length() - 1))) {
					message = formattedMessage + ". " + stacktrace;
				} else {
					message = formattedMessage + ' ' + stacktrace;
				}
				JsonWritingUtils.writeStringField(generator, getFieldName(), message);
			} else if (writeStacktrace) {
				JsonWritingUtils.writeStringField(generator, getFieldName(), stacktrace);
			} else if (writeFormattedMessage) {
				JsonWritingUtils.writeStringField(generator, getFieldName(), formattedMessage);
			} else {
				super.writeTo(generator, event);
			}
		} else {
			super.writeTo(generator, event);
		}
	}

}
