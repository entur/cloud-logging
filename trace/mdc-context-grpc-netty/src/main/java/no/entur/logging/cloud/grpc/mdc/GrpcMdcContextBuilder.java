package no.entur.logging.cloud.grpc.mdc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class GrpcMdcContextBuilder<B extends GrpcMdcContextBuilder<B>> {

	protected Map<String, String> fields;

	public GrpcMdcContextBuilder() {
		this.fields = new HashMap<>();

		GrpcMdcContext grpcMdcContext = GrpcMdcContext.KEY_CONTEXT.get();
		if (grpcMdcContext != null) {
			// already within a context
			withFields(grpcMdcContext.getContext());
		}
	}

	public GrpcMdcContextBuilder(Map<String, String> fields) {
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

	public GrpcMdcContext build() {
		return new GrpcMdcContext(fields);
	}

}