package no.entur.logging.cloud.spring.rr.grpc;

import no.entur.logging.cloud.rr.grpc.GrpcLoggingServerInterceptor;
import no.entur.logging.cloud.rr.grpc.GrpcSink;
import no.entur.logging.cloud.rr.grpc.filter.GrpcServerLoggingFilters;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcMetadataJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcPayloadJsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;

public class OrderedGrpcLoggingServerInterceptor extends GrpcLoggingServerInterceptor implements Ordered {

    protected final int order;

    public OrderedGrpcLoggingServerInterceptor(
            GrpcSink sink,
            GrpcServerLoggingFilters filters,
            GrpcMetadataJsonMapper metadataJsonMapper,
            GrpcPayloadJsonMapper payloadJsonMapper,

            @Value("${entur.logging.request-response.grpc.interceptor-order:0}")
            int order

    ) {
        super(sink, filters, metadataJsonMapper, payloadJsonMapper);
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

}
