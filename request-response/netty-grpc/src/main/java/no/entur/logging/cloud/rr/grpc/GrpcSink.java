package no.entur.logging.cloud.rr.grpc;

import no.entur.logging.cloud.rr.grpc.message.GrpcConnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcDisconnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcRequest;
import no.entur.logging.cloud.rr.grpc.message.GrpcResponse;

public interface GrpcSink {

	boolean isActive();


	void disconnectMessage(GrpcConnect connectMessage, GrpcDisconnect message);

	void connectMessage(GrpcConnect remote);

	void responseMessage(GrpcResponse message);

	void requestMessage(GrpcRequest message);
}
