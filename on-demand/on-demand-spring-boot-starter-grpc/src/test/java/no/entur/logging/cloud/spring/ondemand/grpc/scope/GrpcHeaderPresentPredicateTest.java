package no.entur.logging.cloud.spring.ondemand.grpc.scope;

import io.grpc.Metadata;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GrpcHeaderPresentPredicateTest {

    private static final Metadata.Key<String> DEBUG_KEY =
            Metadata.Key.of("x-debug", Metadata.ASCII_STRING_MARSHALLER);

    private static final Metadata.Key<String> TRACE_KEY =
            Metadata.Key.of("x-trace", Metadata.ASCII_STRING_MARSHALLER);

    @Test
    public void testHeaderPresent_returnsTrue() {
        GrpcHeaderPresentPredicate predicate = new GrpcHeaderPresentPredicate(Set.of("x-debug"));

        Metadata metadata = new Metadata();
        metadata.put(DEBUG_KEY, "true");

        assertTrue(predicate.test(metadata));
    }

    @Test
    public void testHeaderAbsent_returnsFalse() {
        GrpcHeaderPresentPredicate predicate = new GrpcHeaderPresentPredicate(Set.of("x-debug"));

        Metadata metadata = new Metadata();

        assertFalse(predicate.test(metadata));
    }

    @Test
    public void testOtherHeaderPresent_returnsFalse() {
        GrpcHeaderPresentPredicate predicate = new GrpcHeaderPresentPredicate(Set.of("x-debug"));

        Metadata metadata = new Metadata();
        metadata.put(TRACE_KEY, "abc");

        assertFalse(predicate.test(metadata));
    }

    @Test
    public void testOneOfMultipleHeadersPresent_returnsTrue() {
        GrpcHeaderPresentPredicate predicate = new GrpcHeaderPresentPredicate(Set.of("x-debug", "x-trace"));

        Metadata metadata = new Metadata();
        metadata.put(TRACE_KEY, "abc");

        assertTrue(predicate.test(metadata));
    }

    @Test
    public void testEmptyMetadata_returnsFalse() {
        GrpcHeaderPresentPredicate predicate = new GrpcHeaderPresentPredicate(Set.of("x-debug"));

        Metadata metadata = new Metadata();

        assertFalse(predicate.test(metadata));
    }
}
