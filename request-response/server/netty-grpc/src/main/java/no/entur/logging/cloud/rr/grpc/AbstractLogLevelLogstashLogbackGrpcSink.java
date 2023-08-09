package no.entur.logging.cloud.rr.grpc;

import no.entur.logging.cloud.rr.grpc.message.GrpcConnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcDisconnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcRequest;
import no.entur.logging.cloud.rr.grpc.message.GrpcResponse;
import org.slf4j.Marker;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public abstract class AbstractLogLevelLogstashLogbackGrpcSink extends AbstractLogLevelGrpcSink {

    protected final BiConsumer<Marker, String> logConsumer;

    public AbstractLogLevelLogstashLogbackGrpcSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled) {
        super(logLevelEnabled);

        this.logConsumer = logConsumer;
    }

    @Override
    public void connectMessage(GrpcConnect connect) {
        Marker marker = createConnectMarker(connect);
        StringBuilder stringBuilder = new StringBuilder(256);
        connectMessage(connect, stringBuilder);
        logConsumer.accept (marker, stringBuilder.toString());
    }

    protected abstract Marker createConnectMarker(GrpcConnect connect);

    @Override
    public void requestMessage(GrpcRequest message) {
        Marker marker = createRequestMarker(message);
        StringBuilder stringBuilder = new StringBuilder(256);
        requestMessage(message, stringBuilder);
        logConsumer.accept (marker, stringBuilder.toString());
    }

    protected abstract Marker createRequestMarker(GrpcRequest message);

    @Override
    public void responseMessage(GrpcResponse message) {
        Marker marker = createResponseMarker(message);
        StringBuilder stringBuilder = new StringBuilder(256);
        responseMessage(message, stringBuilder);
        logConsumer.accept (marker, stringBuilder.toString());
    }

    protected abstract Marker createResponseMarker(GrpcResponse message);

    @Override
    public void disconnectMessage(GrpcConnect connectMessage, GrpcDisconnect disconnectMessage) {
        Marker marker = createDisconnectMarker(connectMessage, disconnectMessage);
        StringBuilder stringBuilder = new StringBuilder(256);
        disconnectMessage(connectMessage, disconnectMessage, stringBuilder);
        logConsumer.accept (marker, stringBuilder.toString());
    }

    protected abstract Marker createDisconnectMarker(GrpcConnect connectMessage, GrpcDisconnect disconnectMessage);

}
