package no.entur.logging.cloud.grpc.mdc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.util.TransmitStatusRuntimeExceptionInterceptor;
import no.entur.logging.cloud.grpc.mdc.test.GreetingRequest;
import no.entur.logging.cloud.grpc.mdc.test.GreetingServiceGrpc;
import no.entur.logging.cloud.grpc.mdc.test.GreetingServiceGrpc.GreetingServiceBlockingStub;
import no.entur.logging.cloud.grpc.mdc.test.GreetingServiceGrpc.GreetingServiceFutureStub;
import no.entur.logging.cloud.grpc.mdc.test.GreetingServiceGrpc.GreetingServiceStub;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.TimeUnit;

public class AbstractGreetingTest {

	// https://github.com/olivere/grpc-demo/blob/master/java-client/src/main/java/com/altf4/grpc/client/ExampleClient.java
	public static final int MAX_INBOUND_MESSAGE_SIZE = 1 << 20;
	public static final int MAX_OUTBOUND_MESSAGE_SIZE = 1 << 20;
	
	protected GreetingRequest greetingRequest = GreetingRequest.newBuilder().build();

	private static final Integer port = 8097;
   
	private static Server server;

	protected final int maxOutboundMessageSize;
	protected final int maxInboundMessageSize;

	public AbstractGreetingTest() {
		this(MAX_INBOUND_MESSAGE_SIZE, MAX_OUTBOUND_MESSAGE_SIZE);
	}

	public AbstractGreetingTest(int maxInboundMessageSize, int maxOutboundMessageSize) {
		this.maxInboundMessageSize = maxInboundMessageSize;
		this.maxOutboundMessageSize = maxOutboundMessageSize;
	}

	@BeforeAll
	public static void start() throws Exception {

		server = ServerBuilder
				.forPort(port)
				.addService(new GreetingController())
				// reverse order;
				// the status runtime exception interceptor should be the closest to the actual controller
				.intercept(TransmitStatusRuntimeExceptionInterceptor.instance())
				.intercept(new TestMdcContextInterceptor())
				.intercept(InitializeGrpcMdcContextServerInterceptor.newBuilder().build())

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
				.withMaxOutboundMessageSize(maxOutboundMessageSize)
				;
		return greetingService;
	}
	
	
}
