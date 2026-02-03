package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;
import net.logstash.logback.stacktrace.ShortenedThrowableConverter;

import java.io.IOException;
import java.util.Arrays;

public class StackdriverMessageJsonProvider extends MessageJsonProvider {

	private ShortenedThrowableConverter throwableConverter;

	public StackdriverMessageJsonProvider() {
		throwableConverter = new ShortenedThrowableConverter();
		throwableConverter.setMaxLength(24 * 1024);
		throwableConverter.setOmitCommonFrames(true);
		throwableConverter.setShortenedClassNameLength(192);
		throwableConverter.setLineSeparator("\n");
	}

	@Override
	public void start() {
		super.start();
		throwableConverter.start();
	}

	@Override
	public void stop() {
		super.stop();
		throwableConverter.stop();
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
