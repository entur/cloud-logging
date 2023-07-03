package org.entur.auth.grpc;

import org.entur.logging.grpc.JsonPrinterFactory;
import org.entur.logging.grpc.TypeRegistryFactory;
import org.entur.logging.grpc.marker.DefaultGrpcMarkerFactory;
import org.entur.logging.grpc.marker.GrpcResponseMarker;
import org.entur.oidc.grpc.test.GreetingResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static com.google.common.truth.Truth.assertThat;

public class DefaultGrpcMarkerFactoryTest {

    @Test
    public void testOmitsMessageOnGlobalBinaryMaxSize() {
        DefaultGrpcMarkerFactory factory = new DefaultGrpcMarkerFactory(JsonPrinterFactory.createPrinter(true, TypeRegistryFactory.createDefaultTypeRegistry()), Integer.MAX_VALUE, 10);

        GreetingResponse response = GreetingResponse.newBuilder().setMessage("0123456789").setStatus(1).build();

        GrpcResponseMarker marker = (GrpcResponseMarker) factory.createResponseMarker(Collections.emptyMap(), "localhost", "/", response, "local", 123);

        assertThat(marker.getBody()).startsWith("\"Omitted binary message size");
    }

    @Test
    public void testOmitsMessageOnGlobalJsonMaxSize() {
        DefaultGrpcMarkerFactory factory = new DefaultGrpcMarkerFactory(JsonPrinterFactory.createPrinter(true, TypeRegistryFactory.createDefaultTypeRegistry()), 10, Integer.MAX_VALUE);

        GreetingResponse response = GreetingResponse.newBuilder().setMessage("0123456789").setStatus(1).build();

        GrpcResponseMarker marker = (GrpcResponseMarker) factory.createResponseMarker(Collections.emptyMap(), "localhost", "/", response, "local", 123);

        assertThat(marker.getBody()).startsWith("\"Omitted JSON message size");
    }

    @Test
    public void testOmitsMessageDeserializationIfPreviousMessageWasTooBig() {
        DefaultGrpcMarkerFactory factory = new DefaultGrpcMarkerFactory(JsonPrinterFactory.createPrinter(true, TypeRegistryFactory.createDefaultTypeRegistry()), 10, 100);

        GreetingResponse response = GreetingResponse.newBuilder().setMessage("0123456789").setStatus(1).build();

        GrpcResponseMarker first = (GrpcResponseMarker) factory.createResponseMarker(Collections.emptyMap(), "localhost", "/", response, "local", 123);

        assertThat(first.getBody()).startsWith("\"Omitted JSON message size");

        GrpcResponseMarker second = (GrpcResponseMarker) factory.createResponseMarker(Collections.emptyMap(), "localhost", "/", response, "local", 123);
        assertThat(second.getBody()).startsWith("\"Omitted binary message size");

    }

}
