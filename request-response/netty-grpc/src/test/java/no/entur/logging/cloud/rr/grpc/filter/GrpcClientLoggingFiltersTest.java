package no.entur.logging.cloud.rr.grpc.filter;

import org.entur.oidc.grpc.test.GreetingServiceGrpc;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

public class GrpcClientLoggingFiltersTest {

    private final GrpcPayloadFilter anonymize = GrpcPayloadFilter.newBuilder().withAnonymizeKeys("message").build();
    private final GrpcPayloadFilter prune = GrpcPayloadFilter.newBuilder().withPruneKeys("message").build();
    private final GrpcPayloadFilter truncate = GrpcPayloadFilter.newBuilder().withMaxStringLength(16).build();

    @Test
    public void testPayloadFiltersAreIndependentOfLogging() {
        GrpcClientLoggingFilters filters = GrpcClientLoggingFilters.newBuilder()
                .classicDefaultLogging()
                .defaultPayloadFilter(truncate)
                .fullLoggingForService("a", "full")
                .payloadFilterForService("a", anonymize)
                .payloadFilterForService("a", prune, GreetingServiceGrpc.getGreeting1Method())
                .payloadFilterForService("b", null, prune, Set.of("method"))
                .build();

        GrpcLogFilter filter = filters.getFilter("a", "full");
        assertThat(filter.isConnect()).isTrue();
        assertPayloadFilters(filter, anonymize, anonymize);

        filter = filters.getFilter("a", GreetingServiceGrpc.getGreeting1Method().getBareMethodName());
        assertThat(filter.isConnect()).isFalse();
        assertPayloadFilters(filter, prune, prune);

        assertPayloadFilters(filters.getFilter("b", "method"), truncate, prune);
        assertPayloadFilters(filters.getFilter("b", "other"), truncate, truncate);
        assertPayloadFilters(filters.getFilter("other", "method"), truncate, truncate);
    }

    @Test
    public void testWithoutPayloadFilters() {
        GrpcClientLoggingFilters filters = GrpcClientLoggingFilters.newBuilder()
                .classicDefaultLogging()
                .summaryLoggingForService("a", "summary")
                .build();

        assertThat(filters.getFilter("other", "method")).isSameInstanceAs(GrpcLogFilter.REQUEST_RESPONSE);
        assertThat(filters.getFilter("a", "method")).isSameInstanceAs(GrpcLogFilter.REQUEST_RESPONSE);
        assertThat(filters.getFilter("a", "summary")).isSameInstanceAs(GrpcLogFilter.SUMMARY);
    }

    private static void assertPayloadFilters(GrpcLogFilter filter, GrpcPayloadFilter request, GrpcPayloadFilter response) {
        assertThat(filter.getRequestPayloadFilter()).isEqualTo(request);
        assertThat(filter.getResponsePayloadFilter()).isEqualTo(response);
    }
}
