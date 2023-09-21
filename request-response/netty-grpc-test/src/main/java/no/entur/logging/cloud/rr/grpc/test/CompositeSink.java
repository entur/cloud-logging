package no.entur.logging.cloud.rr.grpc.test;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputType;
import no.entur.logging.cloud.rr.grpc.GrpcSink;
import no.entur.logging.cloud.rr.grpc.message.GrpcConnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcDisconnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcRequest;
import no.entur.logging.cloud.rr.grpc.message.GrpcResponse;

/** Sink which effectively adjusts the formatting of the request-response to the current encoder scheme */

public class CompositeSink implements GrpcSink {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private GrpcSink humanReadablePlainSink;
        private GrpcSink humanReadableJsonSink;

        private GrpcSink machineReadableJsonSink;

        public Builder withHumanReadableJsonSink(GrpcSink humanReadableJsonSink) {
            this.humanReadableJsonSink = humanReadableJsonSink;
            return this;
        }

        public Builder withHumanReadablePlainSink(GrpcSink humanReadablePlainSink) {
            this.humanReadablePlainSink = humanReadablePlainSink;
            return this;
        }

        public Builder withMachineReadableJsonSink(GrpcSink machineReadableJsonSink) {
            this.machineReadableJsonSink = machineReadableJsonSink;
            return this;
        }

        public CompositeSink build() {
            if(humanReadablePlainSink == null) {
                throw new IllegalStateException();
            }
            if(humanReadableJsonSink == null) {
                throw new IllegalStateException();
            }
            if(machineReadableJsonSink == null) {
                throw new IllegalStateException();
            }

            return new CompositeSink(humanReadablePlainSink, humanReadableJsonSink, machineReadableJsonSink);
        }
    }

    private GrpcSink humanReadablePlainSink;
    private GrpcSink humanReadableJsonSink;

    private GrpcSink machineReadableJsonSink;

    public CompositeSink(GrpcSink humanReadablePlainSink, GrpcSink humanReadableJsonSink, GrpcSink machineReadableJsonSink) {
        this.humanReadablePlainSink = humanReadablePlainSink;
        this.humanReadableJsonSink = humanReadableJsonSink;
        this.machineReadableJsonSink = machineReadableJsonSink;
    }

    @Override
    public boolean isActive() {
        return getSink().isActive();
    }

    @Override
    public void disconnectMessage(GrpcConnect connectMessage, GrpcDisconnect disconnectMessage) {
        getSink().disconnectMessage(connectMessage, disconnectMessage);
    }

    @Override
    public void connectMessage(GrpcConnect connect) {
        getSink().connectMessage(connect);
    }

    @Override
    public void responseMessage(GrpcResponse message) {
        getSink().responseMessage(message);
    }

    @Override
    public void requestMessage(GrpcRequest message) {
        getSink().requestMessage(message);
    }

    private GrpcSink getSink() {
        CompositeConsoleOutputType output = CompositeConsoleOutputControl.getOutput();
        switch (output) {
            case humanReadablePlain: {
                return humanReadablePlainSink;
            }
            case humanReadableJson: {
                return humanReadableJsonSink;
            }
            case machineReadableJson: {
                return machineReadableJsonSink;
            }
            default : {
                throw new IllegalStateException();
            }
        }
    }


}
