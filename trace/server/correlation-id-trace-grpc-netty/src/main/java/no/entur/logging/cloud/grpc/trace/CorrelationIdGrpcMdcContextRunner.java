package no.entur.logging.cloud.grpc.trace;

import io.grpc.Context;
import no.entur.logging.cloud.grpc.mdc.AbstractGrpcMdcContextBuilder;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContext;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContextRunner;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

public class CorrelationIdGrpcMdcContextRunner extends AbstractGrpcMdcContextBuilder<CorrelationIdGrpcMdcContextRunner> {

	private String correlationId;
	private String requestId;

	public CorrelationIdGrpcMdcContextRunner() {
		super();
	}

	public CorrelationIdGrpcMdcContextRunner(Map<String, String> fields) {
		super(fields);
	}

	public CorrelationIdGrpcMdcContextRunner withCorrelationId(String value) {
		this.correlationId = value;
		return this;
	}

	public CorrelationIdGrpcMdcContextRunner withRequestId(String value) {
		this.requestId = value;
		return this;
	}

	public CorrelationIdGrpcMdcContextRunner withField(String key, String value) {
		switch (key) {
			case GrpcTraceMdcContext.REQUEST_ID_MDC_KEY: {
				requestId = value;
				break;
			}
			case GrpcTraceMdcContext.CORRELATION_ID_MDC_KEY: {
				correlationId = value;
				break;
			}
			default: {
				super.withField(key, value);
			}
		}
		return this;
	}

	public CorrelationIdGrpcMdcContextRunner withFields(Map<String, String> fields) {
		if (!fields.containsKey(GrpcTraceMdcContext.CORRELATION_ID_MDC_KEY) && !fields.containsKey(GrpcTraceMdcContext.REQUEST_ID_MDC_KEY)) {
			super.withFields(fields);
		} else {
			for (Map.Entry<String, String> stringStringEntry : fields.entrySet()) {
				withField(stringStringEntry.getKey(), stringStringEntry.getValue());
			}
		}
		return this;
	}

	public void run(Runnable r) {
		fill();
		// so run in a new context even if there is an existing context,
		// so that the original context object is not touched
		GrpcMdcContextRunner.runInNewContext(fields, r);
	}

	private void fill() {
		if (correlationId == null) {
			correlationId = UUID.randomUUID().toString();
		}

		if (requestId == null) {
			requestId = UUID.randomUUID().toString();
		}

		super.withField(GrpcTraceMdcContext.CORRELATION_ID_MDC_KEY, correlationId);
		super.withField(GrpcTraceMdcContext.REQUEST_ID_MDC_KEY, requestId);
	}

	public <T> T call(Callable<T> r) throws Exception {
		fill();

		// so run in a new context even if there is an existing context,
		// so that the original context object is not touched
		return GrpcMdcContextRunner.callInNewContext(fields, r);
	}
}