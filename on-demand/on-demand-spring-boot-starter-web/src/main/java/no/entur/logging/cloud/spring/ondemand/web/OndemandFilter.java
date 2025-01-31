package no.entur.logging.cloud.spring.ondemand.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import ch.qos.logback.classic.spi.ILoggingEvent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeFactory;
import no.entur.logging.cloud.appender.scope.LoggingScopeSink;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpLoggingScopeFilter;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpLoggingScopeFilters;

import java.io.IOException;
import java.util.Enumeration;
import java.util.function.Predicate;

public class OndemandFilter extends HttpFilter {

    private final String FLUSHED = OndemandFilter.class.getName() + "-flushed";

    private final String FILTER = OndemandFilter.class.getName() + "-filter";
    private final String SCOPE = OndemandFilter.class.getName() + "-scope";

    private final String DURATION = OndemandFilter.class.getName() + "-scope";

    static class ScopeAsyncListener implements AsyncListener {

        private final Runnable runnable;

        public ScopeAsyncListener(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void onComplete(AsyncEvent event) throws IOException {
            runnable.run();
        }

        @Override
        public void onTimeout(AsyncEvent event) {

        }

        @Override
        public void onError(AsyncEvent event) {

        }

        @Override
        public void onStartAsync(AsyncEvent event) {
        }
    }
    private final LoggingScopeSink sink;
    private final HttpLoggingScopeFilters filters;

    private final LoggingScopeFactory loggingScopeFactory;

    public OndemandFilter(LoggingScopeSink sink, HttpLoggingScopeFilters filters, LoggingScopeFactory loggingScopeFactory) {
        this.sink = sink;
        this.filters = filters;
        this.loggingScopeFactory = loggingScopeFactory;
    }

    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse,
                         final FilterChain filterChain) throws ServletException, IOException {

        // ASYNC: this method is invoked twice per invocation, on two different threads
        // make sure to clean up the thread-local map here

        HttpLoggingScopeFilter filter = getHttpLoggingScopeFilter(httpServletRequest);
        LoggingScope scope = getLoggingScope(httpServletRequest, filter);

        try {
            try {
                filterChain.doFilter(httpServletRequest, httpServletResponse);
            } finally {
                if (httpServletRequest.isAsyncStarted()) {

                    // wait for result before deciding to flush
                    httpServletRequest.getAsyncContext().addListener(new ScopeAsyncListener(() -> flush(httpServletRequest, httpServletResponse, filter, scope)));
                } else {
                    flush(httpServletRequest, httpServletResponse, filter, scope);
                }
            }
        } finally {
            // clear thread-local
            loggingScopeFactory.closeScope(scope);
        }
    }

    private LoggingScope getLoggingScope(HttpServletRequest httpServletRequest, HttpLoggingScopeFilter filter) {
        // reuse the logging scope from the first thread (request) in the second thread (response) so that
        // messages are flushed in-order (and we do not create a lot more objects).

        LoggingScope scope = (LoggingScope) httpServletRequest.getAttribute(SCOPE);
        if(scope == null) {
            Predicate<ILoggingEvent> queuePredicate = filter.getQueuePredicate();
            Predicate<ILoggingEvent> ignorePredicate = filter.getIgnorePredicate();

            Predicate<Enumeration<String>> httpHeaderPresentPredicate = filter.getHttpHeaderPresentPredicate();
            if (httpHeaderPresentPredicate.test(httpServletRequest.getHeaderNames())) {
                queuePredicate = filter.getTroubleshootQueuePredicate();
                ignorePredicate = filter.getTroubleshootIgnorePredicate();
            }

            scope = loggingScopeFactory.openScope(queuePredicate, ignorePredicate, filter.getLogLevelFailurePredicate());

            httpServletRequest.setAttribute(SCOPE, scope);
        } else {
            loggingScopeFactory.reopenScope(scope);
        }
        return scope;
    }

    private HttpLoggingScopeFilter getHttpLoggingScopeFilter(HttpServletRequest httpServletRequest) {
        HttpLoggingScopeFilter filter = (HttpLoggingScopeFilter) httpServletRequest.getAttribute(FILTER);
        if(filter == null) {
            filter = filters.getScope(httpServletRequest);
        } else {
            httpServletRequest.setAttribute(FILTER, filter);
        }
        return filter;
    }

    private void flush(HttpServletRequest httpServletRequest, HttpServletResponse servletResponse, HttpLoggingScopeFilter filter, LoggingScope scope) {
        if(httpServletRequest.getAttribute(FLUSHED) == null) {
            httpServletRequest.setAttribute(FLUSHED, Boolean.TRUE);

            if (filter.getHttpStatusFailurePredicate().test(servletResponse.getStatus())) {
                // was there an error response
                sink.write(scope);
            } else if (scope.isFailure()) {
                // was there some dangerous error message?
                sink.write(scope);
            } else if(filter.hasFailureDuration() && System.currentTimeMillis() - scope. getTimestamp() > filter.getFailureDuration()) {
               sink.write(scope);
            }
        }
    }

    public void destroy() {
        // do nothing
    }

}
