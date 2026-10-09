package no.entur.logging.cloud.rr.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import net.logstash.logback.marker.LogstashMarker;
import no.entur.logging.cloud.rr.grpc.filter.GrpcBodyFilter;
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
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.json.JsonFactory;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.truth.Truth.assertThat;

public class DeferredBodyMappingTest {

	private final GreetingRequest greetingRequest = GreetingRequest.newBuilder().setMessage("Hello deferred").build();

	private final JsonFormat.Printer printer = JsonPrinterFactory.createPrinter(false, TypeRegistryFactory.createDefaultTypeRegistry());

	private static final String ASYNC_APPENDER_THREAD_NAME = "test-async-appender";

	private final AtomicInteger mapCount = new AtomicInteger();

	private final List<Thread> mappingThreads = new CopyOnWriteArrayList<>();

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
	public void mapsTooLargeBodiesRightAway() throws Exception {
		// messages larger than the max binary size are only described, so they are mapped (and released) right away
		call(true, new DefaultGrpcPayloadJsonMapper(printer, AbstractGrpcTest.DEFAULT_JSON_MESSAGE_SIZE, 1));

		assertThat(mapCount.get()).isEqualTo(4);
		for (GrpcRequest request : sink.requests) {
			assertThat(request.getPayload().isMapped()).isTrue();
			assertThat(request.getPayload().getMessage()).isNull();
			assertThat(request.getBody()).startsWith("\"Omitted binary message size ");
		}
		for (GrpcResponse response : sink.responses) {
			assertThat(response.getPayload().isMapped()).isTrue();
		}
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

	@Test
	public void markerPostProcessingMapsBeforeAsyncAppenderByDefault() throws Exception {
		call(true);

		GrpcRequest request = sink.requests.get(0);
		GrpcResponse response = sink.responses.get(0);

		// the thread which flushes the on-demand scope
		new GrpcRequestMarker(request).performPostProcessing();
		new GrpcResponseMarker(response).performPostProcessing();

		assertThat(request.getPayload().isMapped()).isTrue();
		assertThat(response.getPayload().isMapped()).isTrue();
		assertThat(mappingThreads).containsExactly(Thread.currentThread(), Thread.currentThread());

		// the async appender's thread only gets the mapped body
		ExecutorService asyncAppender = newAsyncAppenderThread();
		try {
			assertThat(asyncAppender.submit(request::getBody).get()).isEqualTo("{\"message\":\"Hello deferred\"}");
			assertThat(asyncAppender.submit(response::getBody).get()).contains("\"message\":\"Hello\"");
		} finally {
			asyncAppender.shutdown();
		}
		assertThat(mappingThreads).hasSize(2);
	}

	@Test
	public void markerPostProcessingLeavesMappingToAsyncAppenderThreadWhenAllowed() throws Exception {
		call(true, true);

		GrpcRequest request = sink.requests.get(0);
		GrpcResponse response = sink.responses.get(0);

		// the thread which flushes the on-demand scope
		new GrpcRequestMarker(request).performPostProcessing();
		new GrpcResponseMarker(response).performPostProcessing();

		assertThat(request.getPayload().isMapped()).isFalse();
		assertThat(response.getPayload().isMapped()).isFalse();
		assertThat(mapCount.get()).isEqualTo(0);

		// the async appender's thread maps the body when the log statement is written
		ExecutorService asyncAppender = newAsyncAppenderThread();
		try {
			assertThat(asyncAppender.submit(request::getBody).get()).isEqualTo("{\"message\":\"Hello deferred\"}");
			assertThat(asyncAppender.submit(response::getBody).get()).contains("\"message\":\"Hello\"");
		} finally {
			asyncAppender.shutdown();
		}
		assertThat(request.getPayload().isMapped()).isTrue();
		assertThat(response.getPayload().isMapped()).isTrue();
		assertThat(mappingThreads).hasSize(2);
		for (Thread thread : mappingThreads) {
			assertThat(thread.getName()).isEqualTo(ASYNC_APPENDER_THREAD_NAME);
		}
	}

	@Test
	public void markerMapsBodyWhenWrittenIfNotMappedBefore() throws Exception {
		// no post-processing, i.e. as if the async appender's thread is the first to need the body
		call(true, true);

		GrpcRequest request = sink.requests.get(0);
		GrpcResponse response = sink.responses.get(0);

		assertThat(write(new GrpcRequestMarker(request))).contains("\"body\":{\"message\":\"Hello deferred\"}");
		assertThat(write(new GrpcResponseMarker(response))).contains("\"body\":{");

		assertThat(request.getPayload().isMapped()).isTrue();
		assertThat(response.getPayload().isMapped()).isTrue();
		assertThat(mapCount.get()).isEqualTo(2);
	}

	private static String write(LogstashMarker marker) throws Exception {
		StringWriter writer = new StringWriter();
		try (JsonGenerator generator = new JsonFactory().createGenerator(writer)) {
			generator.writeStartObject();
			marker.writeTo(generator);
			generator.writeEndObject();
		}
		return writer.toString();
	}

	@Test
	public void asyncAppenderThreadSettingDoesNotAffectEagerMapping() throws Exception {
		call(false, true);

		assertThat(mapCount.get()).isEqualTo(4);
		for (GrpcRequest request : sink.requests) {
			assertThat(request.getPayload().isMapped()).isTrue();
		}
		for (GrpcResponse response : sink.responses) {
			assertThat(response.getPayload().isMapped()).isTrue();
		}
	}

	private static ExecutorService newAsyncAppenderThread() {
		return Executors.newSingleThreadExecutor(r -> new Thread(r, ASYNC_APPENDER_THREAD_NAME));
	}

	private void call(boolean deferredBodyMapping) throws Exception {
		call(deferredBodyMapping, false);
	}

	private void call(boolean deferredBodyMapping, boolean mapBodyOnAsyncAppenderThread) throws Exception {
		call(deferredBodyMapping, mapBodyOnAsyncAppenderThread, new DefaultGrpcPayloadJsonMapper(printer, AbstractGrpcTest.DEFAULT_JSON_MESSAGE_SIZE, AbstractGrpcTest.DEFAULT_BINARY_MESSAGE_SIZE));
	}

	private void call(boolean deferredBodyMapping, GrpcPayloadJsonMapper delegate) throws Exception {
		call(deferredBodyMapping, false, delegate);
	}

	private void call(boolean deferredBodyMapping, boolean mapBodyOnAsyncAppenderThread, GrpcPayloadJsonMapper delegate) throws Exception {
		GrpcPayloadJsonMapper payloadJsonMapper = new GrpcPayloadJsonMapper() {
			@Override
			public String map(MessageOrBuilder m, GrpcBodyFilter filter) throws InvalidProtocolBufferException {
				mapCount.incrementAndGet();
				mappingThreads.add(Thread.currentThread());
				return delegate.map(m, filter);
			}

			@Override
			public boolean isDeferrable(MessageOrBuilder m) {
				return delegate.isDeferrable(m);
			}
		};
		GrpcMetadataJsonMapper metadataJsonMapper = new DefaultMetadataJsonMapper(new JsonPrinterStatusMapper(printer), new HashMap<>());

		GrpcLoggingServerInterceptor serverInterceptor = GrpcLoggingServerInterceptor.newBuilder()
				.withPayloadJsonMapper(payloadJsonMapper)
				.withMetadataJsonMapper(metadataJsonMapper)
				.withSink(sink)
				.withFilters(GrpcServerLoggingFilters.classic())
				.withDeferredBodyMapping(deferredBodyMapping)
				.withMapBodyOnAsyncAppenderThread(mapBodyOnAsyncAppenderThread)
				.build();

		GrpcLoggingClientInterceptor clientInterceptor = GrpcLoggingClientInterceptor.newBuilder()
				.withPayloadJsonMapper(payloadJsonMapper)
				.withMetadataJsonMapper(metadataJsonMapper)
				.withSink(sink)
				.withFilters(GrpcClientLoggingFilters.classic())
				.withDeferredBodyMapping(deferredBodyMapping)
				.withMapBodyOnAsyncAppenderThread(mapBodyOnAsyncAppenderThread)
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
