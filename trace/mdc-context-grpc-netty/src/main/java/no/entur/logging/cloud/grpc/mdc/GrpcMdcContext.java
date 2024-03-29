package no.entur.logging.cloud.grpc.mdc;

import io.grpc.Context;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Utility class for use holding MDC fields. Note that this exists in parallel to the classic SLF4J MDC.
 * <br>
 * <p>
 * Note 1: Mutable context state is allowed, however the context itself is immutable
 * </p>
 * <p>
 * Note 2: Use of this context must be backed by logging configuration.
 * </p>
 */

public class GrpcMdcContext {

	public static <T> T callInNewContext(GrpcMdcContext mdc, Callable<T> c) throws Exception {
		Context fork = Context.current().withValue(GrpcMdcContext.KEY_CONTEXT, mdc);

		return fork.call(c);
	}

	public static void runInNewContext(GrpcMdcContext mdc, Runnable r) {
		Context fork = Context.current().withValue(GrpcMdcContext.KEY_CONTEXT, mdc);

		fork.run(r);
	}

	public static final Context.Key<GrpcMdcContext> KEY_CONTEXT = Context.key("MDC_CONTEXT");

	public static boolean isWithinContext() {
		return get() != null;
	}

	public static GrpcMdcContextBuilder newContext() {
		return new GrpcMdcContextBuilder();
	}

	public static GrpcMdcContextBuilder newEmptyContext() {
		return new GrpcMdcContextBuilder(new HashMap<>());
	}

	protected Map<String, String> context;

	public static GrpcMdcContext get() {
		return KEY_CONTEXT.get();
	}

	public GrpcMdcContext(Map<String, String> context) {
		if (context == null) {
			throw new IllegalArgumentException();
		}
		this.context = context;
	}

	public GrpcMdcContext(GrpcMdcContext parent) {
		if (parent == null) {
			throw new IllegalArgumentException();
		}
		this.context = parent.getContext();
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

	public Runnable run(Runnable r) {
		// so run in a new context even if there is an existing context, so that the original context object is not touched
		runInNewContext(this, r);
		return r;
	}

	public <T> T call(Callable<T> r) throws Exception {
		// so run in a new context even if there is an existing context,
		// so that the original context object is not touched
		return callInNewContext(this, r);
	}

	public Runnable wrap(Runnable r) {
		// so run in a new context even if there is an existing context, so that the original context object is not touched
		return () -> runInNewContext(this, r);
	}

	public <T> Callable<T> wrap(Callable<T> r) throws Exception {
		// so run in a new context even if there is an existing context,
		// so that the original context object is not touched
		return () -> callInNewContext(this, r);
	}


}
