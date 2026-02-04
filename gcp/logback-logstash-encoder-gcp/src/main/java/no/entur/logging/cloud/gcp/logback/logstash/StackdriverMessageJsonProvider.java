package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.pattern.ExtendedThrowableProxyConverter;
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

        if(throwableConverter == null) {
            throwableConverter = createThrowableConverter();
        }

		if(!throwableConverter.isStarted()) {
			throwableConverter.start();
		}
	}

    protected ThrowableHandlingConverter createThrowableConverter() {
        return new ExtendedThrowableProxyConverter();
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
	public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
		IThrowableProxy throwableProxy = event.getThrowableProxy();
		if (throwableProxy != null) {
			String formattedMessage = event.getFormattedMessage();
			String stacktrace = throwableConverter.convert(event);

			if(formattedMessage != null && stacktrace != null) {
				StringBuilder messageWithStackTrace = new StringBuilder(formattedMessage.length() + 2 + stacktrace.length());

                if(!formattedMessage.isEmpty()) {
                    messageWithStackTrace.append(formattedMessage);
                    if (Character.isLetterOrDigit(formattedMessage.charAt(formattedMessage.length() - 1))) {
                        messageWithStackTrace.append('.');
                    }
                    messageWithStackTrace.append(' ');
                }
				messageWithStackTrace.append(stacktrace);

				JsonWritingUtils.writeStringField(generator, getFieldName(), messageWithStackTrace.toString());
			} else if (stacktrace != null) {
				JsonWritingUtils.writeStringField(generator, getFieldName(), stacktrace);
            } else if (formattedMessage != null) {
                JsonWritingUtils.writeStringField(generator, getFieldName(), formattedMessage);
			}
		} else {
			super.writeTo(generator, event);
		}
	}

    public void setThrowableConverter(ThrowableHandlingConverter throwableConverter) {
        this.throwableConverter = throwableConverter;
    }
}
