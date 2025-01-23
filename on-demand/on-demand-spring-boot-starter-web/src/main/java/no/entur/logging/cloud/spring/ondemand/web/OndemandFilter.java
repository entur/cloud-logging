package no.entur.logging.cloud.spring.ondemand.web;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import ch.qos.logback.classic.spi.ILoggingEvent;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeFactory;
import no.entur.logging.cloud.appender.scope.LoggingScopeSink;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpLoggingScopeFilter;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpLoggingScopeFilters;
import no.entur.logging.cloud.spring.ondemand.web.scope.ThreadLocalLoggingScopeFactory;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class OndemandFilter extends HttpFilter {

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
        HttpLoggingScopeFilter filter = filters.getScope(httpServletRequest);

        Predicate<ILoggingEvent> queuePredicate = filter.getQueuePredicate();
        Predicate<ILoggingEvent> ignorePredicate = filter.getIgnorePredicate();

        Predicate<Enumeration<String>> httpHeaderPresentPredicate = filter.getHttpHeaderPresentPredicate();
        if(httpHeaderPresentPredicate.test(httpServletRequest.getHeaderNames())) {
            queuePredicate = filter.getTroubleshootQueuePredicate();
            ignorePredicate = filter.getTroubleshootIgnorePredicate();
        }

        LoggingScope scope = loggingScopeFactory.openScope(queuePredicate, ignorePredicate, filter.getLogLevelFailurePredicate());
        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } finally {
            if (httpServletRequest.isAsyncStarted()) {
                httpServletRequest.getAsyncContext().addListener(new ScopeAsyncListener(() -> flush(httpServletRequest, httpServletResponse, filter, scope)));
            } else {
                flush(httpServletRequest, httpServletResponse, filter, scope);
            }
        }
    }

    private void flush(HttpServletRequest httpServletRequest, HttpServletResponse servletResponse, HttpLoggingScopeFilter filter, LoggingScope scope) {
        if (filter.getHttpStatusFailurePredicate().test(servletResponse.getStatus())) {
            // was there an error response
            sink.write(scope);
        } else if (scope.isFailure()) {
            // was there some dangerous error message?
            sink.write(scope);
        }
        loggingScopeFactory.closeScope(scope);
    }

    public void destroy() {
    }

}
