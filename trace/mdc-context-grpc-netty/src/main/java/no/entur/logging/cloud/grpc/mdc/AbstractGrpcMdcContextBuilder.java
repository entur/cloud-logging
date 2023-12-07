package no.entur.logging.cloud.grpc.mdc;

import io.grpc.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class AbstractGrpcMdcContextBuilder<B extends AbstractGrpcMdcContextBuilder<B>> {

	protected Map<String, String> fields;

	public AbstractGrpcMdcContextBuilder() {
		GrpcMdcContext grpcMdcContext = GrpcMdcContext.KEY_CONTEXT.get();
		if (grpcMdcContext != null) {
			// already within a context
			this.fields = new HashMap<>(grpcMdcContext.getContext());
		} else {
			this.fields = new HashMap<>();
		}
	}

	public AbstractGrpcMdcContextBuilder(Map<String, String> fields) {
		this.fields = fields;
	}

	public B withFields(Map<String, String> fields) {
		this.fields.putAll(fields);
		return (B) this;
	}

	public B withField(String key, String value) {
		this.fields.put(key, value);
		return (B) this;
	}
}