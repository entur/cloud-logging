package no.entur.logging.cloud.grpc.trace;

import io.grpc.Metadata;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for tracing fields going into the MDC.
 */

public class CorrelationIdGrpcMdcContext extends GrpcMdcContext {

	public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
	public static final Metadata.Key<String> CORRELATION_ID_HEADER_KEY = Metadata.Key.of(CORRELATION_ID_HEADER, Metadata.ASCII_STRING_MARSHALLER);

	public static final String REQUEST_ID_MDC_KEY = "requestId";
	public static final String CORRELATION_ID_MDC_KEY = "correlationId";

	public static CorrelationIdGrpcMdcContextBuilder newContext() {
		return new CorrelationIdGrpcMdcContextBuilder();
	}

	public static CorrelationIdGrpcMdcContextBuilder newEmptyContext() {
		return new CorrelationIdGrpcMdcContextBuilder();
	}

	public CorrelationIdGrpcMdcContext(Map<String, String> context) {
		super(context);
	}

	public CorrelationIdGrpcMdcContext(GrpcMdcContext parent) {
		super(parent);
	}

	public CorrelationIdGrpcMdcContext() {
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
