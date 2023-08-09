package no.entur.logging.cloud.rr.grpc;

import io.grpc.Status;
import no.entur.logging.cloud.rr.grpc.message.GrpcConnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcDisconnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcRequest;
import no.entur.logging.cloud.rr.grpc.message.GrpcResponse;

import java.util.function.BooleanSupplier;

public abstract class AbstractLogLevelGrpcSink implements GrpcSink {

    protected final BooleanSupplier logLevelEnabled;

    public AbstractLogLevelGrpcSink(BooleanSupplier logLevelEnabled) {
        this.logLevelEnabled = logLevelEnabled;
    }

    @Override public boolean isActive() {
        return logLevelEnabled.getAsBoolean();
    }

    protected void requestMessage(GrpcRequest request, StringBuilder messageBuilder) {
        messageBuilder.append("REQ ");
        messageBuilder.append(request.getUri());
        messageBuilder.append(" #");
        messageBuilder.append(request.getNumber());
    }

    protected void responseMessage(GrpcResponse response, StringBuilder messageBuilder) {
        messageBuilder.append("RESP ");
        Status.Code statusCode = response.getStatusCode();
        messageBuilder.append(statusCode.name());
        messageBuilder.append(' ');
        messageBuilder.append(response.getUri());
        messageBuilder.append(" #");
        messageBuilder.append(response.getNumber());
    }

    protected void connectMessage(GrpcConnect request, StringBuilder messageBuilder) {
        messageBuilder.append("CONNECT ");
        messageBuilder.append(request.getUri());
    }

    protected void disconnectMessage(GrpcConnect connectMessage, GrpcDisconnect disconnectMessage, StringBuilder messageBuilder) {
        String verb;
        if (connectMessage == null) {
            messageBuilder.append("SUMMARY ");
        } else {
            messageBuilder.append("DISCONNECT ");
        }
        messageBuilder.append(disconnectMessage.getUri());
        messageBuilder.append(' ');
        int requestCount = disconnectMessage.getRequestCount();
        messageBuilder.append(requestCount);
        if(requestCount > 1) {
            messageBuilder.append(" requests and ");
        } else {
            messageBuilder.append(" request and ");
        }
        int responseCount = disconnectMessage.getResponseCount();
        messageBuilder.append(responseCount);
        if(responseCount > 1) {
            messageBuilder.append(" responses (");
        } else {
            messageBuilder.append(" response (");
        }
        messageBuilder.append(formatBytes(disconnectMessage.getTotalPayloadSize()));
        messageBuilder.append(") in ");
        messageBuilder.append(disconnectMessage.getDuration());
        messageBuilder.append("ms");
    }

    private String formatBytes(long size) {
        if (size < 1024) {
            return size + " bytes";
        }
        if (size < 1024 * 1024) {
            return (size / 1024) + "KB";
        }
        return (size / (1024 * 1024)) + "MB";
    }
}
