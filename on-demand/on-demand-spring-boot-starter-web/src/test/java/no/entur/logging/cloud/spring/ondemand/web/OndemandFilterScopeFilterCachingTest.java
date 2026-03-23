package no.entur.logging.cloud.spring.ondemand.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeFactory;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpLoggingScopeFilter;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpLoggingScopeFilters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for the request-attribute caching in
 * {@link OndemandFilter#doFilter}. On async dispatch the filter is invoked
 * twice on the same request object; {@code filters.getScope()} must therefore
 * be called only once and the result must be cached as a request attribute.
 */
public class OndemandFilterScopeFilterCachingTest {

    private static final String FILTER_ATTR = OndemandFilter.class.getName() + "-filter";
    private static final String SCOPE_ATTR = OndemandFilter.class.getName() + "-scope";

    private HttpLoggingScopeFilters scopeFilters;
    private LoggingScopeFactory loggingScopeFactory;
    private HttpLoggingScopeFilter scopeFilter;
    private LoggingScope loggingScope;
    private OndemandFilter ondemandFilter;

    /** In-memory attribute store that backs the mocked HttpServletRequest. */
    private final Map<String, Object> attributes = new HashMap<>();

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    public void setUp() throws Exception {
        scopeFilters = mock(HttpLoggingScopeFilters.class);
        loggingScopeFactory = mock(LoggingScopeFactory.class);
        scopeFilter = mock(HttpLoggingScopeFilter.class);
        loggingScope = mock(LoggingScope.class);

        // Stub getScope() to return the scope filter
        when(scopeFilters.getScope(any())).thenReturn(scopeFilter);

        // Stub scope factory to open / reopen the logging scope
        when(loggingScopeFactory.openScope(any(), any(), any())).thenReturn(loggingScope);

        // Stub the predicates used inside getLoggingScope
        when(scopeFilter.getQueuePredicate()).thenReturn(e -> true);
        when(scopeFilter.getIgnorePredicate()).thenReturn(e -> false);
        when(scopeFilter.getHttpHeaderPresentPredicate()).thenReturn(names -> false);
        when(scopeFilter.getLogLevelFailurePredicate()).thenReturn(null);
        when(scopeFilter.getHttpStatusFailurePredicate()).thenReturn(status -> false);
        when(scopeFilter.isFailureForDuration(any(long.class))).thenReturn(false);
        when(loggingScope.getTimestamp()).thenReturn(System.currentTimeMillis());

        // Wire request attribute operations to the in-memory map
        request = mock(HttpServletRequest.class);
        when(request.getAttribute(anyString())).thenAnswer(inv -> attributes.get(inv.getArgument(0)));
        org.mockito.Mockito.doAnswer(inv -> {
            attributes.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(request).setAttribute(anyString(), any());
        when(request.isAsyncStarted()).thenReturn(false);
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        response = mock(HttpServletResponse.class);
        when(response.getStatus()).thenReturn(200);

        chain = mock(FilterChain.class);

        ondemandFilter = new OndemandFilter(scopeFilters, loggingScopeFactory);
    }

    @Test
    public void scopeFilterIsLookedUpOnlyOnceAndCachedAsRequestAttribute() throws Exception {
        // First pass (e.g., initial dispatch)
        ondemandFilter.doFilter(request, response, chain);

        // Second pass (e.g., async re-dispatch on the same request object)
        ondemandFilter.doFilter(request, response, chain);

        // filters.getScope() must have been called exactly once; the second
        // pass must have reused the cached request attribute instead.
        verify(scopeFilters, times(1)).getScope(request);

        // The resolved filter must have been stored as a request attribute
        verify(request, times(1)).setAttribute(eq(FILTER_ATTR), eq(scopeFilter));
    }
}
