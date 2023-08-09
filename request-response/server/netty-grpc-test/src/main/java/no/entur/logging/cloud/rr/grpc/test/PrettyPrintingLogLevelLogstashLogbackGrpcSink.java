package no.entur.logging.cloud.rr.grpc.test;

import no.entur.logging.cloud.rr.grpc.AbstractLogLevelLogstashLogbackGrpcSink;
import no.entur.logging.cloud.rr.grpc.AbstractSinkBuilder;
import no.entur.logging.cloud.rr.grpc.marker.GrpcConnectMarker;
import no.entur.logging.cloud.rr.grpc.marker.GrpcDisconnectMarker;
import no.entur.logging.cloud.rr.grpc.message.GrpcConnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcDisconnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcRequest;
import no.entur.logging.cloud.rr.grpc.message.GrpcResponse;
import org.slf4j.Marker;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public class PrettyPrintingLogLevelLogstashLogbackGrpcSink extends AbstractLogLevelLogstashLogbackGrpcSink {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AbstractSinkBuilder<Builder, Builder> {

        public PrettyPrintingLogLevelLogstashLogbackGrpcSink build() {
            if(logger == null) {
                throw new IllegalStateException("Expected logger");
            }
            if(level == null) {
                throw new IllegalStateException("Expected log level");
            }

            return new PrettyPrintingLogLevelLogstashLogbackGrpcSink(loggerToBiConsumer(), logEnabledToBooleanSupplier());
        }

    }

    public PrettyPrintingLogLevelLogstashLogbackGrpcSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled) {
        super(logConsumer, logLevelEnabled);
    }

    @Override
    protected Marker createConnectMarker(GrpcConnect connect) {
        return new GrpcConnectMarker(connect);
    }

    @Override
    protected Marker createRequestMarker(GrpcRequest message) {
        return new PrettyPrintingRequestSingleFieldAppendingMarker(message);
    }

    @Override
    protected Marker createResponseMarker(GrpcResponse message) {
        return new PrettyPrintingResponseSingleFieldAppendingMarker(message);
    }

    @Override
    protected Marker createDisconnectMarker(GrpcConnect connectMessage, GrpcDisconnect disconnectMessage) {
        return new GrpcDisconnectMarker(disconnectMessage);
    }
}
