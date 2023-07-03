package org.entur.logging.grpc.mdc;

import io.grpc.Context;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Utility class for use holding MDC fields.<br>
 * <p>
 * Note: Mutable context state is allowed, however the context itself is immutable
 * </p>
 */

public class GrpcMdcContext {

	public static ContextWrapper newContext() {
		return new ContextWrapper();
	}

	public static class ContextWrapper {

		private Map<String, String> fields;

		public ContextWrapper() {
			GrpcMdcContext grpcMdcContext = GrpcMdcContext.KEY_CONTEXT.get();
			if (grpcMdcContext != null) {
				// already within a context
				this.fields = new HashMap<>(grpcMdcContext.getContext());
			} else {
				this.fields = new HashMap<>();
			}
		}

		public ContextWrapper withFields(Map<String, String> fields) {
			this.fields.putAll(fields);
			return this;
		}

		public ContextWrapper withField(String key, String value) {
			this.fields.put(key, value);
			return this;
		}

		public void run(Runnable r) {
			// so run in a new context even if there is an existing context, so that the original context object is not touched
			runInNewContext(fields, r);
		}

		public <T> T call(Callable<T> r) throws Exception {
			// so run in a new context even if there is an existing context,
			// so that the original context object is not touched
			return callInNewContext(fields, r);
		}
	}


	public static final Context.Key<GrpcMdcContext> KEY_CONTEXT = Context.key("MDC_CONTEXT");

	public static <T> T callInNewContext(Map<String, String> mdc, Callable<T> c) throws Exception {
		Context fork = Context.current().withValue(GrpcMdcContext.KEY_CONTEXT, new GrpcMdcContext(mdc));

		return fork.call(c);
	}

	public static void runInNewContext(Map<String, String> mdc, Runnable r) {
		Context fork = Context.current().withValue(GrpcMdcContext.KEY_CONTEXT, new GrpcMdcContext(mdc));

		fork.run(r);
	}

	protected Map<String, String> context;

	public static boolean isWithinContext() {
		return get() != null;
	}

	public static GrpcMdcContext get() {
		return KEY_CONTEXT.get();
	}

	public GrpcMdcContext(Map<String, String> context) {
		if (context == null) {
			throw new IllegalArgumentException();
		}
		this.context = context;
	}

	public GrpcMdcContext() {
		this(new HashMap<>());
	}

	public Map<String, String> getContext() {
		return context;
	}

	public void setContext(Map<String, String> context) {
		this.context = context;
	}

	@Override
	public String toString() {
		return super.toString() + "{" +
				context +
				'}';
	}

	public String get(String key) {
		return context.get(key);
	}

	public void remove(String key) {
		context.remove(key);
	}

	public boolean containsKey(String key) {
		return context.containsKey(key);
	}

	public void removeAll(Collection<String> keys) {
		for (String key : keys) {
			context.remove(key);
		}
	}

	public String put(String key, String value) {
		return context.put(key, value);
	}

	public void putAll(Map<String, String> map) {
		context.putAll(map);
	}

	public void clear() {
		context.clear();
	}


}
