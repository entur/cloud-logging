package no.entur.logging.cloud.rr.grpc;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import no.entur.logging.cloud.rr.grpc.filter.GrpcClientLoggingFilters;
import no.entur.logging.cloud.rr.grpc.filter.GrpcPayloadFilter;
import no.entur.logging.cloud.rr.grpc.filter.GrpcServerLoggingFilters;
import no.entur.logging.cloud.rr.grpc.mapper.DefaultMetadataJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcMetadataJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcPayloadJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.ProtobufJsonWriterFactory;
import no.entur.logging.cloud.rr.grpc.mapper.ProtobufJsonWriterGrpcPayloadJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.ProtobufJsonWriterStatusMapper;
import no.entur.logging.cloud.rr.grpc.mapper.TranscodingProtobufJsonWriterFactory;
import no.entur.logging.cloud.rr.grpc.mapper.TypeRegistryFactory;
import no.entur.logging.cloud.rr.grpc.message.GrpcConnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcDisconnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcRequest;
import no.entur.logging.cloud.rr.grpc.message.GrpcResponse;
import org.entur.oidc.grpc.test.GreetingRequest;
import org.entur.oidc.grpc.test.GreetingServiceGrpc;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;

/**
 * Payload filters per method, for both the server and the client interceptor.
 */

public class GrpcPayloadFilterLoggingTest {

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

        List<String> requestBodies() {
            return requests.stream().map(GrpcRequest::getBody).collect(Collectors.toList());
        }

        List<String> responseBodies() {
            return responses.stream().map(GrpcResponse::getBody).collect(Collectors.toList());
        }
    }

    @Test
    public void testPayloadFilterPerMethod() throws Exception {
        ProtobufJsonWriterFactory factory = new TranscodingProtobufJsonWriterFactory(TypeRegistryFactory.createDefaultTypeRegistry(), 1024);
        GrpcPayloadJsonMapper payloadJsonMapper = new ProtobufJsonWriterGrpcPayloadJsonMapper(factory);
        GrpcMetadataJsonMapper metadataJsonMapper = new DefaultMetadataJsonMapper(new ProtobufJsonWriterStatusMapper(factory.create(GrpcPayloadFilter.NONE)), new HashMap<>());

        GrpcPayloadFilter anonymize = GrpcPayloadFilter.newBuilder().withAnonymizeKeys("message").build();
        GrpcPayloadFilter prune = GrpcPayloadFilter.newBuilder().withPruneKeys("message").build();

        CapturingGrpcSink serverSink = new CapturingGrpcSink();
        GrpcLoggingServerInterceptor serverInterceptor = GrpcLoggingServerInterceptor.newBuilder()
                .withPayloadJsonMapper(payloadJsonMapper)
                .withMetadataJsonMapper(metadataJsonMapper)
                .withSink(serverSink)
                .withFilters(GrpcServerLoggingFilters.newBuilder()
                        .classicDefaultLogging()
                        .payloadFilterForService(GreetingServiceGrpc.SERVICE_NAME, anonymize, GreetingServiceGrpc.getGreeting1Method())
                        .build())
                .build();

        CapturingGrpcSink clientSink = new CapturingGrpcSink();
        GrpcLoggingClientInterceptor clientInterceptor = GrpcLoggingClientInterceptor.newBuilder()
                .withPayloadJsonMapper(payloadJsonMapper)
                .withMetadataJsonMapper(metadataJsonMapper)
                .withSink(clientSink)
                .withFilters(GrpcClientLoggingFilters.newBuilder()
                        .classicDefaultLogging()
                        .payloadFilterForService(GreetingServiceGrpc.SERVICE_NAME, prune, GreetingServiceGrpc.getGreeting1Method())
                        .build())
                .build();

        String serverName = InProcessServerBuilder.generateName();
        Server server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(ServerInterceptors.intercept(new GreetingController(), serverInterceptor))
                .build()
                .start();
        ManagedChannel channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .intercept(clientInterceptor)
                .build();
        try {
            GreetingServiceGrpc.GreetingServiceBlockingStub stub = GreetingServiceGrpc.newBlockingStub(channel);

            GreetingRequest request = GreetingRequest.newBuilder().setMessage("secret").build();

            assertThat(stub.greeting1(request).getMessage()).isEqualTo("Hello");
            assertThat(stub.greeting5(request).getMessage()).isEqualTo("Hello");
        } finally {
            channel.shutdownNow();
            server.shutdownNow();
        }

        assertThat(serverSink.requestBodies()).containsExactly("{\"message\":\"*\"}", "{\"message\":\"secret\"}").inOrder();
        assertThat(serverSink.responseBodies().get(0)).contains("\"message\":\"*\"");
        assertThat(serverSink.responseBodies().get(1)).contains("\"message\":\"Hello\"");

        assertThat(clientSink.requestBodies()).containsExactly("{\"message\":\"[removed]\"}", "{\"message\":\"secret\"}").inOrder();
        assertThat(clientSink.responseBodies().get(0)).contains("\"message\":\"[removed]\"");
        assertThat(clientSink.responseBodies().get(1)).contains("\"message\":\"Hello\"");
    }
}
