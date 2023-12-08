package no.entur.logging.cloud.grpc.trace;

import io.grpc.Metadata;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContext;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContextRunner;
import org.slf4j.MDC;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Utility class for tracing fields going into the MDC.
 */

public class GrpcTraceMdcContext extends GrpcMdcContext {

	public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
	public static final Metadata.Key<String> CORRELATION_ID_HEADER_KEY = Metadata.Key.of(CORRELATION_ID_HEADER, Metadata.ASCII_STRING_MARSHALLER);

	public static final String REQUEST_ID_MDC_KEY = "requestId";
	public static final String CORRELATION_ID_MDC_KEY = "correlationId";

	public static CorrelationIdGrpcMdcContextRunner newCorrelationIdContext() {
		return new CorrelationIdGrpcMdcContextRunner();
	}

	public void setCorrelationId(String value) {
		put(CORRELATION_ID_MDC_KEY, value);
	}

	public String getCorrelationId() {
		return get(CORRELATION_ID_MDC_KEY);
	}

	public void setRequestId(String value) {
		put(REQUEST_ID_MDC_KEY, value);
	}

	public String getRequestId() {
		return get(REQUEST_ID_MDC_KEY);
	}

}
