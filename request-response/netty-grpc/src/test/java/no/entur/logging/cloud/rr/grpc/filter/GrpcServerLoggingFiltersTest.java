package no.entur.logging.cloud.rr.grpc.filter;

import org.entur.oidc.grpc.test.GreetingServiceGrpc;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

public class GrpcServerLoggingFiltersTest {

    private final GrpcPayloadFilter anonymize = GrpcPayloadFilter.newBuilder().withAnonymizeKeys("message").build();
    private final GrpcPayloadFilter prune = GrpcPayloadFilter.newBuilder().withPruneKeys("message").build();
    private final GrpcPayloadFilter truncate = GrpcPayloadFilter.newBuilder().withMaxStringLength(16).build();

    @Test
    public void testWithoutPayloadFilters() {
        GrpcServerLoggingFilters filters = GrpcServerLoggingFilters.newBuilder()
                .classicDefaultLogging()
                .fullLoggingForService("a")
                .summaryLoggingForService("a", "summary")
                .build();

        assertThat(filters.getFilter("other", "method")).isSameInstanceAs(GrpcLogFilter.REQUEST_RESPONSE);
        assertThat(filters.getFilter("a", "method")).isSameInstanceAs(GrpcLogFilter.FULL);
        assertThat(filters.getFilter("a", "summary")).isSameInstanceAs(GrpcLogFilter.SUMMARY);

        GrpcLogFilter filter = filters.getFilter("a", "method");
        assertThat(filter.getRequestPayloadFilter()).isEqualTo(GrpcPayloadFilter.NONE);
        assertThat(filter.getResponsePayloadFilter()).isEqualTo(GrpcPayloadFilter.NONE);
    }

    @Test
    public void testPayloadFiltersAreIndependentOfLogging() {
        GrpcServerLoggingFilters filters = GrpcServerLoggingFilters.newBuilder()
                .classicDefaultLogging()
                .fullLoggingForService("a")
                .summaryLoggingForService("a", "summary")
                .payloadFilterForService("a", anonymize)
                .payloadFilterForService("a", prune, "pruned")
                .payloadFilterForService("b", prune, "pruned")
                .build();

        // service payload filter, service logging
        GrpcLogFilter filter = filters.getFilter("a", "method");
        assertThat(filter.isConnect()).isTrue();
        assertPayloadFilters(filter, anonymize, anonymize);

        // service payload filter, method logging
        filter = filters.getFilter("a", "summary");
        assertThat(filter.isRequest()).isFalse();
        assertThat(filter.isDisconnect()).isTrue();
        assertPayloadFilters(filter, anonymize, anonymize);

        // method payload filter, service logging
        filter = filters.getFilter("a", "pruned");
        assertThat(filter.isConnect()).isTrue();
        assertPayloadFilters(filter, prune, prune);

        // method payload filter, default logging
        filter = filters.getFilter("b", "pruned");
        assertThat(filter.isConnect()).isFalse();
        assertThat(filter.isRequest()).isTrue();
        assertThat(filter.isResponse()).isTrue();
        assertPayloadFilters(filter, prune, prune);

        // no payload filter for other methods
        assertThat(filters.getFilter("b", "method")).isSameInstanceAs(GrpcLogFilter.REQUEST_RESPONSE);
        assertThat(filters.getFilter("other", "method")).isSameInstanceAs(GrpcLogFilter.REQUEST_RESPONSE);
    }

    @Test
    public void testDefaultPayloadFilter() {
        GrpcServerLoggingFilters filters = GrpcServerLoggingFilters.newBuilder()
                .classicDefaultLogging()
                .defaultPayloadFilter(truncate)
                .noLoggingForService("a", "none")
                .payloadFilterForService("a", prune, GreetingServiceGrpc.getGreeting1Method())
                .build();

        assertPayloadFilters(filters.getFilter("other", "method"), truncate, truncate);
        assertPayloadFilters(filters.getFilter("a", "method"), truncate, truncate);
        assertPayloadFilters(filters.getFilter("a", GreetingServiceGrpc.getGreeting1Method().getBareMethodName()), prune, prune);
        assertThat(filters.getFilter("a", "none").isLogging()).isFalse();
    }

    @Test
    public void testRequestAndResponsePayloadFiltersFallBackIndependently() {
        GrpcServerLoggingFilters filters = GrpcServerLoggingFilters.newBuilder()
                .classicDefaultLogging()
                .defaultPayloadFilter(truncate, null)
                .payloadFilterForService("a", anonymize, null, Set.of())
                .payloadFilterForService("a", null, prune, Set.of("method"))
                .build();

        assertPayloadFilters(filters.getFilter("other", "method"), truncate, GrpcPayloadFilter.NONE);
        assertPayloadFilters(filters.getFilter("a", "other"), anonymize, GrpcPayloadFilter.NONE);
        assertPayloadFilters(filters.getFilter("a", "method"), anonymize, prune);
    }

    @Test
    public void testPayloadFiltersTakePrecedenceOverLogFilter() {
        GrpcLogFilter logFilter = GrpcLogFilter.FULL.withPayloadFilters(anonymize, anonymize);

        GrpcServerLoggingFilters filters = GrpcServerLoggingFilters.newBuilder()
                .classicDefaultLogging()
                .loggingForService("a", logFilter)
                .payloadFilterForService("a", null, prune, Set.of("method"))
                .build();

        assertThat(filters.getFilter("a", "other")).isSameInstanceAs(logFilter);
        assertPayloadFilters(filters.getFilter("a", "method"), anonymize, prune);
    }

    private static void assertPayloadFilters(GrpcLogFilter filter, GrpcPayloadFilter request, GrpcPayloadFilter response) {
        assertThat(filter.getRequestPayloadFilter()).isEqualTo(request);
        assertThat(filter.getResponsePayloadFilter()).isEqualTo(response);
    }
}
