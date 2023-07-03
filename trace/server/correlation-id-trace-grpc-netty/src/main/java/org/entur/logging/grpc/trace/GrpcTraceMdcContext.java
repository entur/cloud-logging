package org.entur.logging.grpc.trace;

import io.grpc.Metadata;
import org.slf4j.MDC;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Utility class for tracing fields going into the MDC.
 */

public class GrpcTraceMdcContext implements AutoCloseable {

	public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
	public static final Metadata.Key<String> CORRELATION_ID_HEADER_KEY = Metadata.Key.of(CORRELATION_ID_HEADER, Metadata.ASCII_STRING_MARSHALLER);

	public static final String REQUEST_ID_MDC_KEY = "requestId";
	public static final String CORRELATION_ID_MDC_KEY = "correlationId";

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {
		private String correlationId;
		private String requestId;

		private Map<String, String> context = new HashMap<>(); // thread safe for reading

		public Builder withCorrelationId(String value) {
			this.correlationId = value;
			return this;
		}

		public Builder withRequestId(String value) {
			this.requestId = value;
			return this;
		}

		public Builder withField(String key, String value) {
			switch (key) {
				case REQUEST_ID_MDC_KEY: {
					requestId = value;
					break;
				}
				case CORRELATION_ID_MDC_KEY: {
					correlationId = value;
					break;
				}
				default: {
					context.put(key, value);
				}
			}
			return this;
		}

		public Builder withFields(Map<String, String> fields) {
			if (!fields.containsKey(CORRELATION_ID_MDC_KEY) && !fields.containsKey(REQUEST_ID_MDC_KEY)) {
				context.putAll(fields);
			} else {
				for (Entry<String, String> stringStringEntry : fields.entrySet()) {
					withField(stringStringEntry.getKey(), stringStringEntry.getValue());
				}
			}
			return this;
		}

		/**
		 * Build {@linkplain GrpcTraceMdcContext}.
		 *
		 * @return thread safe (for reading) {@linkplain GrpcTraceMdcContext} instance, i.e. can be passed from one to multiple threads.
		 */

		public GrpcTraceMdcContext build() {
			if (correlationId == null) {
				correlationId = UUID.randomUUID().toString();
			}

			if (requestId == null) {
				requestId = UUID.randomUUID().toString();
			}

			context.put(CORRELATION_ID_MDC_KEY, correlationId);
			context.put(REQUEST_ID_MDC_KEY, requestId);

			return new GrpcTraceMdcContext(context);
		}
	}

	public static GrpcTraceMdcContext getContext(Metadata headers) {
		Builder builder = newBuilder();
		String header = headers.get(CORRELATION_ID_HEADER_KEY);
		if (header != null) {
			builder.withCorrelationId(sanitize(header));
		}
		return builder.build();
	}

	public static String sanitize(String inputValue) {
		// https://en.wikipedia.org/wiki/HTTP_response_splitting
		if (!containsNumbersLowercaseLettersAndDashes(inputValue)) {
			try {
				return URLEncoder.encode(inputValue, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e); // should never happen
			}
		}
		return inputValue;
	}

	public static boolean containsNumbersLowercaseLettersAndDashes(String inputValue) {
		for (int i = 0; i < inputValue.length(); i++) {
			char c = inputValue.charAt(i);
			if (!Character.isDigit(c) && c != '-' && (c < 'a' || c > 'z')) {
				return false;
			}
		}
		return true;
	}

	protected final Map<String, String> context;

	protected GrpcTraceMdcContext(Map<String, String> context) {
		this.context = context;
	}

	public AutoCloseable put() {
		for (Entry<String, String> entry : context.entrySet()) {
			MDC.put(entry.getKey(), entry.getValue());
		}
		return this;
	}

	public void clear() {
		for (Entry<String, String> entry : context.entrySet()) {
			MDC.remove(entry.getKey());
		}
	}

	public Map<String, String> getContext() {
		return context;
	}

	@Override
	public void close() throws Exception {
		clear();
	}
}
