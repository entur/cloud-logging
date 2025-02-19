package no.entur.logging.cloud.gcp.trace.spring.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class GcpTraceMdcSupportBuilder {

	protected static final boolean[] VALUE_CHARACTERS;

	static {
		// create lookup-table
		// accept digits, a-z and dash
		int lowercaseStart = 'a';
		int lowercaseEnd = 'z';

		int digitStart = '0';
		int digitEnd = '9';

		boolean[] values = new boolean[lowercaseEnd + 1];

		for (int i = lowercaseStart; i <= lowercaseEnd; i++) {
			values[i] = true;
		}
		for (int i = digitStart; i <= digitEnd; i++) {
			values[i] = true;
		}

		// special chars
		values['-'] = true;

		VALUE_CHARACTERS = values;
	}

	protected String correlationId;
	protected String requestId;
	protected String spanId;
	protected String trace;

	protected boolean sanitize;

	public GcpTraceMdcSupportBuilder() {
	}

	public GcpTraceMdcSupportBuilder withSpanId(String spanId) {
		this.spanId = spanId;
		return this;
	}

	public GcpTraceMdcSupportBuilder withTrace(String trace) {
		this.trace = trace;
		return this;
	}

	public GcpTraceMdcSupportBuilder withSanitize(boolean sanitize) {
		this.sanitize = sanitize;
		return this;
	}

	public GcpTraceMdcSupportBuilder withCorrelationId(String value) {
		this.correlationId = value;
		return this;
	}

	public GcpTraceMdcSupportBuilder withRequestId(String value) {
		this.requestId = value;
		return this;
	}

	public GcpTraceMdcSupport build() {
		if (correlationId == null) {
			correlationId = UUID.randomUUID().toString();
		} else if(sanitize) {
			correlationId = sanitize(correlationId);
		}

		if (requestId == null) {
			requestId = UUID.randomUUID().toString();
		} else if(sanitize) {
			requestId = sanitize(requestId);
		}

		if (trace == null) {
			trace = correlationId; // assumes logging to standard output
		} else if(sanitize) {
			trace = sanitize(trace);
		}

		if (spanId == null) {
			//  16-character, hexadecimal encoding of an 8-byte array
			ThreadLocalRandom current = ThreadLocalRandom.current();
			long random = current.nextLong();
			spanId = Long.toHexString(random);
		} else if(sanitize) {
			spanId = sanitize(spanId); // TODO really just HEX characters
		}

		return new GcpTraceMdcSupport(correlationId, requestId, trace, spanId);
	}

	public static String sanitize(String inputValue) {
		// https://en.wikipedia.org/wiki/HTTP_response_splitting
		if(!containsNumbersLowercaseLettersAndDashes(inputValue)) {
			return URLEncoder.encode(inputValue, StandardCharsets.UTF_8);
		}
		return inputValue;
	}

	public static boolean containsNumbersLowercaseLettersAndDashes(String inputValue) {
		for(int i = 0; i < inputValue.length(); i++) {
			char c = inputValue.charAt(i);

			if (c >= VALUE_CHARACTERS.length) {
				return false;
			}
			if (!VALUE_CHARACTERS[c]) {
				return false;
			}
		}
		return true;
	}

}