package no.entur.logging.cloud.rr.grpc;

import no.entur.logging.cloud.rr.grpc.message.GrpcConnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcDisconnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcRequest;
import no.entur.logging.cloud.rr.grpc.message.GrpcResponse;
import no.entur.logging.cloud.rr.grpc.marker.GrpcConnectMarker;
import no.entur.logging.cloud.rr.grpc.marker.GrpcDisconnectMarker;
import no.entur.logging.cloud.rr.grpc.marker.GrpcRequestMarker;
import no.entur.logging.cloud.rr.grpc.marker.GrpcResponseMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import static org.slf4j.event.EventConstants.DEBUG_INT;
import static org.slf4j.event.EventConstants.ERROR_INT;
import static org.slf4j.event.EventConstants.INFO_INT;
import static org.slf4j.event.EventConstants.TRACE_INT;
import static org.slf4j.event.EventConstants.WARN_INT;

/**
 *
 * Default mark + log message sink
 *
 */
public class LogbackLogstashGrpcSink extends AbstractLogLevelLogstashLogbackGrpcSink {
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AbstractSinkBuilder<Builder, Builder> {

        public LogbackLogstashGrpcSink build() {
            if(logger == null) {
                logger = LoggerFactory.getLogger(GrpcLoggingServerInterceptor.class);
            }

            if(level == null) {
                level = Level.DEBUG;
            }

            return new LogbackLogstashGrpcSink(loggerToBiConsumer(), logEnabledToBooleanSupplier());
        }

    }

    public LogbackLogstashGrpcSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled) {
        super(logConsumer, logLevelEnabled);
    }

    @Override
    protected Marker createConnectMarker(GrpcConnect connect) {
        return new GrpcConnectMarker(connect);
    }

    @Override
    protected Marker createRequestMarker(GrpcRequest request) {
        return new GrpcRequestMarker(request);
    }

    @Override
    protected Marker createResponseMarker(GrpcResponse response) {
        return new GrpcResponseMarker(response);
    }

    @Override
    protected Marker createDisconnectMarker(GrpcConnect connectMessage, GrpcDisconnect disconnectMessage) {
        return new GrpcDisconnectMarker(disconnectMessage);
    }

}
