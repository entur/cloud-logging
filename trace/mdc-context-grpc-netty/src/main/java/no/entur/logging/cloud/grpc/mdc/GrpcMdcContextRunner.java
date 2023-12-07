package no.entur.logging.cloud.grpc.mdc;

import io.grpc.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class GrpcMdcContextRunner extends AbstractGrpcMdcContextBuilder<GrpcMdcContextRunner> {

	public static <T> T callInNewContext(Map<String, String> mdc, Callable<T> c) throws Exception {
		Context fork = Context.current().withValue(GrpcMdcContext.KEY_CONTEXT, new GrpcMdcContext(mdc));

		return fork.call(c);
	}

	public static void runInNewContext(Map<String, String> mdc, Runnable r) {
		Context fork = Context.current().withValue(GrpcMdcContext.KEY_CONTEXT, new GrpcMdcContext(mdc));

		fork.run(r);
	}

	public GrpcMdcContextRunner() {
		super();
	}

	public GrpcMdcContextRunner(Map<String, String> fields) {
		super(fields);
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