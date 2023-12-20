package no.entur.logging.cloud.trace.spring.grpc.interceptor;

import no.entur.logging.cloud.grpc.trace.CorrelationIdGrpcMdcContextServerInterceptor;
import org.springframework.core.Ordered;

public class OrderedCorrelationIdGrpcMdcContextServerInterceptor extends CorrelationIdGrpcMdcContextServerInterceptor implements Ordered {

    private int order;

    public OrderedCorrelationIdGrpcMdcContextServerInterceptor(boolean required, boolean response, CorrelationIdListener correlationIdListener, int order) {
        super(required, response, correlationIdListener);
        this.order = order;
    }

    public OrderedCorrelationIdGrpcMdcContextServerInterceptor(int order) {
        super();
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
