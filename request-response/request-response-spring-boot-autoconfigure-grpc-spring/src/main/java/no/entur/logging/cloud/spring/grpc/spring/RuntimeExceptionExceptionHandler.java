package no.entur.logging.cloud.spring.grpc.spring;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;

/**
 *
 * Catch all exception-handler. Does not handle {@link StatusRuntimeException}.
 *
 */

public class RuntimeExceptionExceptionHandler implements GrpcExceptionHandler, Ordered {

    private final int order;

    public RuntimeExceptionExceptionHandler(int order) {
        this.order = order;
    }

    @Override
	public @Nullable StatusException handleException(Throwable e) {
		if (e instanceof RuntimeException s) {
            if(e instanceof StatusRuntimeException) {
                // handled by framework
                return null;
            }
            return Status.INTERNAL.withDescription(s.getMessage()).asException();
        }
		return null;
	}

    @Override
    public int getOrder() {
        return order;
    }
}
