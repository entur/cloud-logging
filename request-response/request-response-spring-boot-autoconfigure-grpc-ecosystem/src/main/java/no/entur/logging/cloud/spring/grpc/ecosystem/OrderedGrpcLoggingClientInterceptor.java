package no.entur.logging.cloud.spring.grpc.ecosystem;

import no.entur.logging.cloud.rr.grpc.GrpcLoggingClientInterceptor;
import no.entur.logging.cloud.rr.grpc.GrpcSink;
import no.entur.logging.cloud.rr.grpc.filter.GrpcClientLoggingFilters;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcMetadataJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcPayloadJsonMapper;
import org.springframework.core.Ordered;

public class OrderedGrpcLoggingClientInterceptor extends GrpcLoggingClientInterceptor implements Ordered {

    protected final int order;
    public OrderedGrpcLoggingClientInterceptor(GrpcSink sink, GrpcClientLoggingFilters filters, GrpcMetadataJsonMapper metadataJsonMapper, GrpcPayloadJsonMapper payloadJsonMapper, int order) {
        super(sink, filters, metadataJsonMapper, payloadJsonMapper);
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
