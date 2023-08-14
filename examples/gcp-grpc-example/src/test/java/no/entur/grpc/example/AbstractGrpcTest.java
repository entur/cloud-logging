package no.entur.grpc.example;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.util.TransmitStatusRuntimeExceptionInterceptor;

import no.entur.logging.cloud.grpc.mdc.GrpcMdcContextInterceptor;
import no.entur.logging.cloud.grpc.trace.GrpcAddMdcTraceToResponseInterceptor;
import no.entur.logging.cloud.grpc.trace.GrpcTraceMdcContextInterceptor;
import no.entur.logging.cloud.rr.grpc.GrpcLoggingServerInterceptor;
import no.entur.logging.cloud.rr.grpc.filter.GrpcServerLoggingFilters;
import org.entur.grpc.example.GreetingRequest;
import org.entur.grpc.example.GreetingServiceGrpc;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.lognet.springboot.grpc.context.LocalRunningGrpcPort;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.concurrent.TimeUnit;

public class AbstractGrpcTest {

	// https://github.com/olivere/grpc-demo/blob/master/java-client/src/main/java/com/altf4/grpc/client/ExampleClient.java
	public static final int MAX_INBOUND_MESSAGE_SIZE = 1 << 20;
	public static final int MAX_OUTBOUND_MESSAGE_SIZE = 1 << 20;

	protected GreetingRequest greetingRequest = GreetingRequest.newBuilder().build();

	@LocalRunningGrpcPort
	protected int port;


	protected final int maxOutboundMessageSize;
	protected final int maxInboundMessageSize;

	public AbstractGrpcTest() {
		this(MAX_INBOUND_MESSAGE_SIZE, MAX_OUTBOUND_MESSAGE_SIZE);
	}

	public AbstractGrpcTest(int maxInboundMessageSize, int maxOutboundMessageSize) {
		this.maxInboundMessageSize = maxInboundMessageSize;
		this.maxOutboundMessageSize = maxOutboundMessageSize;
	}

	protected GreetingServiceGrpc.GreetingServiceBlockingStub stub() {
		ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
		GreetingServiceGrpc.GreetingServiceBlockingStub greetingService = GreetingServiceGrpc.newBlockingStub(managedChannel);
		return greetingService;
	}

	protected void shutdown(GreetingServiceGrpc.GreetingServiceBlockingStub stub) throws InterruptedException {
		ManagedChannel m = (ManagedChannel)stub.getChannel();
		m.shutdown();
		m.awaitTermination(15, TimeUnit.SECONDS);
	}

	protected GreetingServiceGrpc.GreetingServiceFutureStub futureStub() {
		ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
		return GreetingServiceGrpc.newFutureStub(managedChannel);
	}

	protected static void shutdown(GreetingServiceGrpc.GreetingServiceFutureStub stub) throws InterruptedException {
		ManagedChannel m = (ManagedChannel)stub.getChannel();
		m.shutdown();
		m.awaitTermination(15, TimeUnit.SECONDS);
	}

	protected void shutdown(GreetingServiceGrpc.GreetingServiceStub stub) throws InterruptedException {
		ManagedChannel m = (ManagedChannel)stub.getChannel();
		m.shutdown();
		m.awaitTermination(15, TimeUnit.SECONDS);
	}


	protected GreetingServiceGrpc.GreetingServiceStub async() {
		ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
		GreetingServiceGrpc.GreetingServiceStub greetingService = GreetingServiceGrpc.newStub(managedChannel)
				.withMaxInboundMessageSize(maxInboundMessageSize)
				.withMaxOutboundMessageSize(maxOutboundMessageSize);
		return greetingService;
	}


}
