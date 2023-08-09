package no.entur.logging.cloud.rr.grpc;

import com.google.protobuf.util.JsonFormat;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.util.TransmitStatusRuntimeExceptionInterceptor;
import no.entur.logging.cloud.rr.grpc.filter.GrpcServerLoggingFilters;
import no.entur.logging.cloud.rr.grpc.mapper.DefaultGrpcPayloadJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.DefaultMetadataJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcMetadataJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcStatusMapper;
import no.entur.logging.cloud.rr.grpc.mapper.JsonPrinterFactory;
import no.entur.logging.cloud.rr.grpc.mapper.JsonPrinterStatusMapper;
import no.entur.logging.cloud.rr.grpc.mapper.TypeRegistryFactory;
import org.entur.oidc.grpc.test.GreetingRequest;
import org.entur.oidc.grpc.test.GreetingServiceGrpc;
import org.entur.oidc.grpc.test.GreetingServiceGrpc.GreetingServiceBlockingStub;
import org.entur.oidc.grpc.test.GreetingServiceGrpc.GreetingServiceFutureStub;
import org.entur.oidc.grpc.test.GreetingServiceGrpc.GreetingServiceStub;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class AbstractGrpcTest {

	// https://github.com/olivere/grpc-demo/blob/master/java-client/src/main/java/com/altf4/grpc/client/ExampleClient.java
	public static final int MAX_INBOUND_MESSAGE_SIZE = 1 << 20;
	public static final int MAX_OUTBOUND_MESSAGE_SIZE = 1 << 20;

	public static final int DEFAULT_JSON_MESSAGE_SIZE = 99 * 1024;
	public static final int DEFAULT_BINARY_MESSAGE_SIZE = 40 * 1024;


	protected GreetingRequest greetingRequest = GreetingRequest.newBuilder().build();

	private final static Integer port = 8097;

	private static Server server;

	protected final int maxOutboundMessageSize;
	protected final int maxInboundMessageSize;

	public AbstractGrpcTest() {
		this(MAX_INBOUND_MESSAGE_SIZE, MAX_OUTBOUND_MESSAGE_SIZE);
	}

	public AbstractGrpcTest(int maxInboundMessageSize, int maxOutboundMessageSize) {
		this.maxInboundMessageSize = maxInboundMessageSize;
		this.maxOutboundMessageSize = maxOutboundMessageSize;
	}

	@BeforeAll
	public static void start() throws Exception {
		JsonFormat.Printer printer = JsonPrinterFactory.createPrinter(false, TypeRegistryFactory.createDefaultTypeRegistry());

		DefaultGrpcPayloadJsonMapper grpcPayloadJsonMapper = new DefaultGrpcPayloadJsonMapper(printer, DEFAULT_JSON_MESSAGE_SIZE, DEFAULT_BINARY_MESSAGE_SIZE);

		GrpcStatusMapper grpcStatusMapper = new JsonPrinterStatusMapper(printer);

		GrpcMetadataJsonMapper grpcMetadataJsonMapper = new DefaultMetadataJsonMapper(grpcStatusMapper, new HashMap<>());

		GrpcSink sink = LogbackLogstashGrpcSink.newBuilder().build();

		GrpcLoggingServerInterceptor grpcLoggingServerInterceptor = GrpcLoggingServerInterceptor
				.newBuilder()
				.withPayloadJsonMapper(grpcPayloadJsonMapper)
				.withMetadataJsonMapper(grpcMetadataJsonMapper)
				.withSink(sink)
				.withFilters(GrpcServerLoggingFilters
						.newBuilder()
						.classicDefaultLogging()
						.fullLoggingForPrefix("/org.entur.oidc.grpc.test.GreetingService/greeting3")
						.fullLoggingForPrefix("/org.entur.oidc.grpc.test.GreetingService/fullLogging")
						.summaryLoggingForPrefix("/org.entur.oidc.grpc.test.GreetingService/summaryLogging")
						.noLoggingForPrefix("/org.entur.oidc.grpc.test.GreetingService/noLogging")
						.build())
				.build();

		server = ServerBuilder
				.forPort(port)
				.addService(new GreetingController())
				// reverse order;
				// the status runtime exception interceptor should be the closest to the actual controller
				.intercept(new MyValidationServerInterceptor())
				.intercept(TransmitStatusRuntimeExceptionInterceptor.instance())
				.intercept(grpcLoggingServerInterceptor)
				.build();

		server.start();
	}

	@AfterAll
	public static  void stop() throws InterruptedException {
		if(server != null) {
			server.shutdown();
			server.awaitTermination(5, TimeUnit.SECONDS);
		}
	}

	protected GreetingServiceBlockingStub stub() {
		ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
		GreetingServiceBlockingStub greetingService = GreetingServiceGrpc.newBlockingStub(managedChannel);
		return greetingService;
	}

	protected void shutdown(GreetingServiceBlockingStub stub) throws InterruptedException {
		ManagedChannel m = (ManagedChannel)stub.getChannel();
		m.shutdown();
		m.awaitTermination(15, TimeUnit.SECONDS);
	}

	protected GreetingServiceFutureStub futureStub() {
		ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
		return GreetingServiceGrpc.newFutureStub(managedChannel);
	}

	protected void shutdown(GreetingServiceFutureStub stub) throws InterruptedException {
		ManagedChannel m = (ManagedChannel)stub.getChannel();
		m.shutdown();
		m.awaitTermination(15, TimeUnit.SECONDS);
	}

	protected void shutdown(GreetingServiceStub stub) throws InterruptedException {
		ManagedChannel m = (ManagedChannel)stub.getChannel();
		m.shutdown();
		m.awaitTermination(15, TimeUnit.SECONDS);
	}


	protected GreetingServiceStub async() {
		ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
		GreetingServiceStub greetingService = GreetingServiceGrpc.newStub(managedChannel)
				.withMaxInboundMessageSize(maxInboundMessageSize)
				.withMaxOutboundMessageSize(maxOutboundMessageSize);
		return greetingService;
	}


}
