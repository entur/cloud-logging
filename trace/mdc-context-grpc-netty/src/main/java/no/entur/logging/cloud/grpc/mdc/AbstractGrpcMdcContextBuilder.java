package no.entur.logging.cloud.grpc.mdc;

import io.grpc.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class AbstractGrpcMdcContextBuilder<B extends AbstractGrpcMdcContextBuilder<B>> {

	protected Map<String, String> fields;

	public AbstractGrpcMdcContextBuilder() {
		this.fields = new HashMap<>();

		GrpcMdcContext grpcMdcContext = GrpcMdcContext.KEY_CONTEXT.get();
		if (grpcMdcContext != null) {
			// already within a context
			withFields(grpcMdcContext.getContext());
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

	public abstract GrpcMdcContext build();

	public void run(Runnable r) {
		// so run in a new context even if there is an existing context, so that the original context object is not touched
		build().run(r);
	}

	public <T> T call(Callable<T> r) throws Exception {
		// so run in a new context even if there is an existing context,
		// so that the original context object is not touched
		return build().call(r);
	}
}