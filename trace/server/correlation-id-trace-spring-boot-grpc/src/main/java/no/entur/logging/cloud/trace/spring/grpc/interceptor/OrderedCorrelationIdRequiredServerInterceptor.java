package no.entur.logging.cloud.trace.spring.grpc.interceptor;

import no.entur.logging.cloud.grpc.trace.CorrelationIdRequiredServerInterceptor;
import org.springframework.core.Ordered;

public class OrderedCorrelationIdRequiredServerInterceptor extends CorrelationIdRequiredServerInterceptor implements Ordered {

    private final int order;

    public OrderedCorrelationIdRequiredServerInterceptor(int order) {
        this(true, order);
    }

    public OrderedCorrelationIdRequiredServerInterceptor(boolean enabled, int order) {
        super(enabled);
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
