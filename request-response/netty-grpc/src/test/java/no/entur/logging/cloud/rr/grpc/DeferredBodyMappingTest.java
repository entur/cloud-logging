package no.entur.logging.cloud.rr.grpc;

import com.google.protobuf.util.JsonFormat;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import no.entur.logging.cloud.rr.grpc.filter.GrpcClientLoggingFilters;
import no.entur.logging.cloud.rr.grpc.filter.GrpcServerLoggingFilters;
import no.entur.logging.cloud.rr.grpc.mapper.DefaultGrpcPayloadJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.DefaultMetadataJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcMetadataJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcPayloadJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.JsonPrinterFactory;
import no.entur.logging.cloud.rr.grpc.mapper.JsonPrinterStatusMapper;
import no.entur.logging.cloud.rr.grpc.mapper.TypeRegistryFactory;
import no.entur.logging.cloud.rr.grpc.marker.GrpcRequestMarker;
import no.entur.logging.cloud.rr.grpc.marker.GrpcResponseMarker;
import no.entur.logging.cloud.rr.grpc.message.GrpcConnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcDisconnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcRequest;
import no.entur.logging.cloud.rr.grpc.message.GrpcResponse;
import org.entur.oidc.grpc.test.GreetingRequest;
import org.entur.oidc.grpc.test.GreetingResponse;
import org.entur.oidc.grpc.test.GreetingServiceGrpc;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.truth.Truth.assertThat;

public class DeferredBodyMappingTest {

	private final GreetingRequest greetingRequest = GreetingRequest.newBuilder().setMessage("Hello deferred").build();

	private final JsonFormat.Printer printer = JsonPrinterFactory.createPrinter(false, TypeRegistryFactory.createDefaultTypeRegistry());

	private final AtomicInteger mapCount = new AtomicInteger();

	private final CapturingGrpcSink sink = new CapturingGrpcSink();

	@Test
	public void mapsBodiesRightAway() throws Exception {
		call(false);

		// server + client, request + response
		assertThat(mapCount.get()).isEqualTo(4);
		assertThat(sink.requests).hasSize(2);
		assertThat(sink.responses).hasSize(2);

		for (GrpcRequest request : sink.requests) {
			assertThat(request.getPayload().isMapped()).isTrue();
		}
		for (GrpcResponse response : sink.responses) {
			assertThat(response.getPayload().isMapped()).isTrue();
		}
	}

	@Test
	public void defersMappingUntilBodyIsRead() throws Exception {
		call(true);

		assertThat(mapCount.get()).isEqualTo(0);
		assertThat(sink.requests).hasSize(2);
		assertThat(sink.responses).hasSize(2);

		for (GrpcRequest request : sink.requests) {
			assertThat(request.getPayload().isMapped()).isFalse();
			assertThat(request.getBody()).isEqualTo("{\"message\":\"Hello deferred\"}");
		}
		for (GrpcResponse response : sink.responses) {
			assertThat(response.getPayload().isMapped()).isFalse();
			assertThat(response.getBody()).contains("\"message\":\"Hello\"");
		}

		assertThat(mapCount.get()).isEqualTo(4);
	}

	@Test
	public void markerPostProcessingMapsDeferredBody() throws Exception {
		call(true);

		GrpcRequest request = sink.requests.get(0);
		new GrpcRequestMarker(request).performPostProcessing();
		assertThat(request.getPayload().isMapped()).isTrue();

		GrpcResponse response = sink.responses.get(0);
		new GrpcResponseMarker(response).performPostProcessing();
		assertThat(response.getPayload().isMapped()).isTrue();

		assertThat(mapCount.get()).isEqualTo(2);
	}

	private void call(boolean deferredBodyMapping) throws Exception {
		GrpcPayloadJsonMapper delegate = new DefaultGrpcPayloadJsonMapper(printer, AbstractGrpcTest.DEFAULT_JSON_MESSAGE_SIZE, AbstractGrpcTest.DEFAULT_BINARY_MESSAGE_SIZE);
		GrpcPayloadJsonMapper payloadJsonMapper = (m, filter) -> {
			mapCount.incrementAndGet();
			return delegate.map(m, filter);
		};
		GrpcMetadataJsonMapper metadataJsonMapper = new DefaultMetadataJsonMapper(new JsonPrinterStatusMapper(printer), new HashMap<>());

		GrpcLoggingServerInterceptor serverInterceptor = GrpcLoggingServerInterceptor.newBuilder()
				.withPayloadJsonMapper(payloadJsonMapper)
				.withMetadataJsonMapper(metadataJsonMapper)
				.withSink(sink)
				.withFilters(GrpcServerLoggingFilters.classic())
				.withDeferredBodyMapping(deferredBodyMapping)
				.build();

		GrpcLoggingClientInterceptor clientInterceptor = GrpcLoggingClientInterceptor.newBuilder()
				.withPayloadJsonMapper(payloadJsonMapper)
				.withMetadataJsonMapper(metadataJsonMapper)
				.withSink(sink)
				.withFilters(GrpcClientLoggingFilters.classic())
				.withDeferredBodyMapping(deferredBodyMapping)
				.build();

		Server server = ServerBuilder.forPort(0)
				.addService(new GreetingController())
				.intercept(serverInterceptor)
				.build()
				.start();

		ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", server.getPort())
				.usePlaintext()
				.intercept(clientInterceptor)
				.build();
		try {
			GreetingResponse response = GreetingServiceGrpc.newBlockingStub(channel).greeting1(greetingRequest);
			assertThat(response.getMessage()).isEqualTo("Hello");
		} finally {
			channel.shutdown();
			channel.awaitTermination(15, TimeUnit.SECONDS);
			server.shutdown();
			server.awaitTermination(15, TimeUnit.SECONDS);
		}
	}

	private static class CapturingGrpcSink implements GrpcSink {

		private final List<GrpcRequest> requests = new CopyOnWriteArrayList<>();
		private final List<GrpcResponse> responses = new CopyOnWriteArrayList<>();

		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public void disconnectMessage(GrpcConnect connectMessage, GrpcDisconnect message) {
		}

		@Override
		public void connectMessage(GrpcConnect remote) {
		}

		@Override
		public void responseMessage(GrpcResponse message) {
			responses.add(message);
		}

		@Override
		public void requestMessage(GrpcRequest message) {
			requests.add(message);
		}
	}
}
