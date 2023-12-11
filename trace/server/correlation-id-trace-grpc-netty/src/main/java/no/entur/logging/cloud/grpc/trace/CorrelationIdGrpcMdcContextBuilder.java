package no.entur.logging.cloud.grpc.trace;

import no.entur.logging.cloud.grpc.mdc.AbstractGrpcMdcContextBuilder;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContextBuilder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

public class CorrelationIdGrpcMdcContextBuilder extends GrpcMdcContextBuilder<CorrelationIdGrpcMdcContextBuilder> {

	private String correlationId;
	private String requestId;

	public CorrelationIdGrpcMdcContextBuilder() {
		super();
	}

	public CorrelationIdGrpcMdcContextBuilder(Map<String, String> fields) {
		super(fields);
	}

	public CorrelationIdGrpcMdcContextBuilder withCorrelationId(String value) {
		this.correlationId = value;
		return this;
	}

	public CorrelationIdGrpcMdcContextBuilder withRequestId(String value) {
		this.requestId = value;
		return this;
	}

	public CorrelationIdGrpcMdcContextBuilder withField(String key, String value) {
		switch (key) {
			case CorrelationIdGrpcMdcContext.REQUEST_ID_MDC_KEY: {
				requestId = value;
				break;
			}
			case CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY: {
				correlationId = value;
				break;
			}
			default: {
				super.withField(key, value);
			}
		}
		return this;
	}

	public CorrelationIdGrpcMdcContextBuilder withFields(Map<String, String> fields) {
		if (!fields.containsKey(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY) && !fields.containsKey(CorrelationIdGrpcMdcContext.REQUEST_ID_MDC_KEY)) {
			super.withFields(fields);
		} else {
			for (Map.Entry<String, String> stringStringEntry : fields.entrySet()) {
				withField(stringStringEntry.getKey(), stringStringEntry.getValue());
			}
		}
		return this;
	}

	public CorrelationIdGrpcMdcContext build() {
		if (correlationId == null) {
			correlationId = UUID.randomUUID().toString();
		}

		if (requestId == null) {
			requestId = UUID.randomUUID().toString();
		}

		super.withField(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY, correlationId);
		super.withField(CorrelationIdGrpcMdcContext.REQUEST_ID_MDC_KEY, requestId);

		return new CorrelationIdGrpcMdcContext(fields);
	}

}