package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;

import java.io.IOException;

public class StackdriverMessageJsonProvider extends MessageJsonProvider {

	@Override
	public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
		IThrowableProxy throwableProxy = event.getThrowableProxy();
		if (throwableProxy != null) {
			String formattedMessage = event.getFormattedMessage();
			String message = throwableProxy.getMessage();        	
			StringBuilder messageWithStackTrace = new StringBuilder();
			if(formattedMessage != null && !formattedMessage.isEmpty() && message != null && !message.isEmpty()) {
				messageWithStackTrace.append(formattedMessage);
				
				if(Character.isLetterOrDigit(formattedMessage.charAt(formattedMessage.length() - 1))) {
					messageWithStackTrace.append('.');
				}
				
				messageWithStackTrace.append(' ');
				messageWithStackTrace.append(message);
				messageWithStackTrace.append('\n');
			} else if(formattedMessage != null && !formattedMessage.isEmpty()) {
				messageWithStackTrace.append(formattedMessage);
				messageWithStackTrace.append('\n');
			} else if(message != null && !message.isEmpty()) {
				messageWithStackTrace.append(message);
				messageWithStackTrace.append('\n');
			}

			writeStack(throwableProxy, "", messageWithStackTrace);

			JsonWritingUtils.writeStringField(generator, getFieldName(), messageWithStackTrace.toString());
		} else {
			super.writeTo(generator, event);
		}
	}
	
	/**
	 * Format stack-trace
	 * 
	 * @see https://github.com/GoogleCloudPlatform/google-cloud-java/tree/master/google-cloud-contrib/google-cloud-logging-logback
	 */

	static void writeStack(IThrowableProxy throwProxy, String prefix, StringBuilder payload) {
		if (throwProxy == null) {
			return;
		}
		payload
			.append(prefix)
			.append(throwProxy.getClassName())
			.append(": ")
			.append(throwProxy.getMessage())
			.append('\n');
		StackTraceElementProxy[] trace = throwProxy.getStackTraceElementProxyArray();
		if (trace == null) {
			trace = new StackTraceElementProxy[0];
		}

		int commonFrames = throwProxy.getCommonFrames();
		int printFrames = trace.length - commonFrames;
		for (int i = 0; i < printFrames; i++) {
			payload.append("    ").append(trace[i]).append('\n');
		}
		if (commonFrames != 0) {
			payload.append("    ... ").append(commonFrames).append(" common frames elided\n");
		}

		writeStack(throwProxy.getCause(), "caused by: ", payload);
	}
}
