package no.entur.logging.cloud.trace.spring.grpc.interceptor;

import no.entur.logging.cloud.grpc.trace.CopyCorrelationIdFromGrpcMdcContextToRequestClientInterceptor;
import org.springframework.core.Ordered;

public class OrderedCopyCorrelationIdFromGrpcMdcContextToRequestClientInterceptor extends CopyCorrelationIdFromGrpcMdcContextToRequestClientInterceptor implements Ordered {

    private final int order;

    public OrderedCopyCorrelationIdFromGrpcMdcContextToRequestClientInterceptor(int order) {
        super();
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
