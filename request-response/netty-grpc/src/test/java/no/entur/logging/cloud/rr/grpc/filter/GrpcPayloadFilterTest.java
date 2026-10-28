package no.entur.logging.cloud.rr.grpc.filter;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GrpcPayloadFilterTest {

    @Test
    public void testNone() {
        assertThat(GrpcPayloadFilter.NONE.isNone()).isTrue();
        assertThat(GrpcPayloadFilter.newBuilder().build()).isEqualTo(GrpcPayloadFilter.NONE);
        assertThat(GrpcPayloadFilter.newBuilder().withAnonymizeKeys("password").build().isNone()).isFalse();
        assertThat(GrpcPayloadFilter.newBuilder().withMaxStringLength(16).build().isNone()).isFalse();
    }

    @Test
    public void testKeysAreAnyPaths() {
        GrpcPayloadFilter filter = GrpcPayloadFilter.newBuilder()
                .withAnonymizeKeys("password")
                .withPruneKeys("document")
                .build();

        assertThat(filter.getAnonymizePaths()).containsExactly("$..password");
        assertThat(filter.getPrunePaths()).containsExactly("$..document");
    }

    @Test
    public void testEquals() {
        GrpcPayloadFilter a = GrpcPayloadFilter.newBuilder().withAnonymizeKeys("password").withMaxStringLength(16).build();
        GrpcPayloadFilter b = GrpcPayloadFilter.newBuilder().withAnonymizePaths("$..password").withMaxStringLength(16).build();
        GrpcPayloadFilter c = GrpcPayloadFilter.newBuilder().withPruneKeys("password").withMaxStringLength(16).build();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    public void testInvalidConfigurationFailsOnBuild() {
        assertThrows(IllegalArgumentException.class, () -> GrpcPayloadFilter.newBuilder().withAnonymizePaths("not a path").build());
        assertThrows(IllegalArgumentException.class, () -> GrpcPayloadFilter.newBuilder().withMaxStringLength(-2).build());
    }
}
