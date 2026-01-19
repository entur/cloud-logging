package no.entur.logging.cloud.spring.grpc.ecosystem;

import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;

public class StatusRuntimeExceptionExceptionHandler implements GrpcExceptionHandler, Ordered {

    private final int order;

    public StatusRuntimeExceptionExceptionHandler(int order) {
        this.order = order;
    }

    @Override
	public @Nullable StatusException handleException(Throwable e) {
		if (e instanceof StatusRuntimeException s) {
            return s.getStatus().withDescription(e.getMessage()).asException();
        }
		return null;
	}

    @Override
    public int getOrder() {
        return order;
    }
}
