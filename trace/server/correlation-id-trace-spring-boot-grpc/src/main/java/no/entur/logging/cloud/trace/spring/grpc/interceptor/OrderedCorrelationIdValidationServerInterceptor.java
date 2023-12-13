package no.entur.logging.cloud.trace.spring.grpc.interceptor;

import no.entur.logging.cloud.grpc.trace.CorrelationIdValidationServerInterceptor;
import org.springframework.core.Ordered;

public class OrderedCorrelationIdValidationServerInterceptor extends CorrelationIdValidationServerInterceptor implements Ordered {

    private final int order;

    public OrderedCorrelationIdValidationServerInterceptor(int order) {
        this(true, order);
    }

    public OrderedCorrelationIdValidationServerInterceptor(boolean enabled, int order) {
        super(enabled);
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
