package no.entur.grpc.example.without.test.artifacts.otel.starter;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import no.entur.logging.cloud.gcp.logback.logstash.StackdriverMicrometerTraceMdcJsonProvider;
import no.entur.logging.cloud.logback.logstash.test.junit.LogStatement;
import no.entur.logging.cloud.logback.logstash.test.junit.LogStatements;
import org.entur.grpc.example.GreetingRequest;
import org.entur.grpc.example.GreetingServiceGrpc;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Value;
import static com.google.common.truth.Truth.assertThat;

import java.util.concurrent.TimeUnit;

public class SpringAbstractGrpcTest {

	// https://github.com/olivere/grpc-demo/blob/master/java-client/src/main/java/com/altf4/grpc/client/ExampleClient.java
	public static final int MAX_INBOUND_MESSAGE_SIZE = 1 << 20;
	public static final int MAX_OUTBOUND_MESSAGE_SIZE = 1 << 20;

	protected GreetingRequest greetingRequest = GreetingRequest.newBuilder().build();

	@Value("${grpc.server.port:9090}")
	protected int port;

	protected final int maxOutboundMessageSize;
	protected final int maxInboundMessageSize;

	public SpringAbstractGrpcTest() {
		this(MAX_INBOUND_MESSAGE_SIZE, MAX_OUTBOUND_MESSAGE_SIZE);
	}

	public SpringAbstractGrpcTest(int maxInboundMessageSize, int maxOutboundMessageSize) {
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


	public static void assertGcpTrace(LogStatements statements) {
		// Wait a bit to ensure that the logs have been flushed and captured
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		Assertions.assertFalse(statements.isEmpty(), "Expected log statements to be captured, but none were found.");
		for (LogStatement statement : statements) {
			assertThat(statement.getJsonPropertyString(StackdriverMicrometerTraceMdcJsonProvider.GCP_TRACE_KEY)).hasLength(32);
			assertThat(statement.getJsonPropertyString(StackdriverMicrometerTraceMdcJsonProvider.GCP_SPAN_ID_KEY)).hasLength(16);
		}
	}
}
