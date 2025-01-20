package no.entur.logging.cloud.logbook.filter.path;

import no.entur.logging.cloud.logbook.filter.JsonMaxValueLengthBodyFilter;
import no.entur.logging.cloud.logbook.filter.path.strategy.JsonOnlyMatchPathStrategy;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static org.mockito.Mockito.*;

public class RequestResponseMatcherResponseFilterTest {


    @Test
    public void testBuilder() throws Exception {
        String requestBody = IOUtils.resourceToString("/input/request.json", StandardCharsets.UTF_8);
        String responseBody = IOUtils.resourceToString("/input/response.json", StandardCharsets.UTF_8);

        List<Predicate<String>> baseline = Arrays.asList( (path) -> path.startsWith("/api"));
        JsonOnlyMatchPathStrategy strategy = new JsonOnlyMatchPathStrategy(baseline, baseline);

        HttpRequest request = mock(HttpRequest.class);
        when(request.getPath()).thenReturn("/api/customer/add");
        when(request.getContentType()).thenReturn("application/json");
        when(request.withBody()).thenReturn(request);
        when(request.getBodyAsString()).thenReturn(requestBody);

        HttpResponse response = mock(HttpResponse.class);
        when(response.withBody()).thenReturn(response);
        when(response.getContentType()).thenReturn("application/json");
        when(response.getBodyAsString()).thenReturn(responseBody);

        HttpRequest httpRequest = strategy.process(request);
        HttpResponse httpResponse = strategy.process(request, response);

        RequestMatcherRequestFilter requestFilter = RequestMatcherRequestFilter.newPathPrefixBuilder().withPathPrefixFilter("/api/customer", new JsonMaxValueLengthBodyFilter(7)).build();
        RequestResponseMatcherResponseFilter responseFilter = RequestResponseMatcherResponseFilter.newPathPrefixBuilder().withPathPrefixFilter("/api/customer", new JsonMaxValueLengthBodyFilter(8)).build();

        HttpRequest filteredRequest = requestFilter.filter(httpRequest);
        HttpResponse filteredResponse = responseFilter.filter(httpResponse);

        Assertions.assertEquals(filteredRequest.getBodyAsString(), IOUtils.resourceToString("/output/request.json", StandardCharsets.UTF_8));
        Assertions.assertEquals(filteredResponse.getBodyAsString(), IOUtils.resourceToString("/output/response.json", StandardCharsets.UTF_8));
    }

}
