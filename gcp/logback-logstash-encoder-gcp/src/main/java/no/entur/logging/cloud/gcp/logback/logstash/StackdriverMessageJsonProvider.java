package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import com.fasterxml.jackson.core.JsonGenerator;
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
		if(!throwableConverter.isStarted()) {
			throwableConverter.start();
		}
	}

	@Override
	public void stop() {
		super.stop();
		if(throwableConverter.isStarted()) {
			throwableConverter.stop();
		}
	}

	@Override
	public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
		IThrowableProxy throwableProxy = event.getThrowableProxy();
		if (throwableProxy != null) {
			String formattedMessage = event.getFormattedMessage();

			String stacktrace = throwableConverter.convert(event);

			if(formattedMessage != null) {
				StringBuilder messageWithStackTrace = new StringBuilder(formattedMessage.length() + 2 + stacktrace.length());

				messageWithStackTrace.append(formattedMessage);
				if (Character.isLetterOrDigit(formattedMessage.charAt(formattedMessage.length() - 1))) {
					messageWithStackTrace.append('.');
				}
				messageWithStackTrace.append(' ');
				messageWithStackTrace.append(throwableConverter.convert(event));

				JsonWritingUtils.writeStringField(generator, getFieldName(), messageWithStackTrace.toString());
			} else {
				JsonWritingUtils.writeStringField(generator, getFieldName(), stacktrace);
			}
		} else {
			super.writeTo(generator, event);
		}
	}

}
