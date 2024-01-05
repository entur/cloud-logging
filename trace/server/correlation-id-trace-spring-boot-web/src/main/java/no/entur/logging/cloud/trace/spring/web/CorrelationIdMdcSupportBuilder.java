package no.entur.logging.cloud.trace.spring.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class CorrelationIdMdcSupportBuilder {

	protected String correlationId;
	protected String requestId;

	protected boolean sanitize;

	public CorrelationIdMdcSupportBuilder() {
	}

	public CorrelationIdMdcSupportBuilder withSanitize(boolean sanitize) {
		this.sanitize = sanitize;
		return this;
	}

	public CorrelationIdMdcSupportBuilder withCorrelationId(String value) {
		this.correlationId = value;
		return this;
	}

	public CorrelationIdMdcSupportBuilder withRequestId(String value) {
		this.requestId = value;
		return this;
	}

	public CorrelationIdMdcSupport build() {
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

		return new CorrelationIdMdcSupport(correlationId, requestId);
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
			if(!Character.isDigit(c) && c != '-' && (c < 'a'|| c > 'z')) {
				return false;
			}
		}
		return true;
	}

}