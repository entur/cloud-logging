package no.entur.logging.cloud.grpc.mdc;

import io.grpc.Context;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for use holding MDC fields.<br>
 * <p>
 * Note: Mutable context state is allowed, however the context itself is immutable
 * </p>
 */

public class GrpcMdcContext {

	public static final Context.Key<GrpcMdcContext> KEY_CONTEXT = Context.key("MDC_CONTEXT");

	public static boolean isWithinContext() {
		return get() != null;
	}

	public static GrpcMdcContextRunner newContext() {
		return new GrpcMdcContextRunner();
	}

	public static GrpcMdcContextRunner newEmptyContext() {
		return new GrpcMdcContextRunner(new HashMap<>());
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


}
