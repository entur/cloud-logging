package no.entur.logging.cloud.gcp.spring.web;

import ch.qos.logback.classic.spi.ILoggingEvent;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeSink;
import no.entur.logging.cloud.gcp.spring.web.scope.HttpLoggingScopeFilter;
import no.entur.logging.cloud.gcp.spring.web.scope.HttpLoggingScopeFilters;
import no.entur.logging.cloud.gcp.spring.web.scope.ThreadLocalLoggingScopeFactory;

import java.io.IOException;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

public class OndemandFilter implements Filter {

	private final LoggingScopeSink sink;
	private final HttpLoggingScopeFilters filters;

	private final ThreadLocalLoggingScopeFactory loggingScopeFactory;

	public OndemandFilter(LoggingScopeSink sink, HttpLoggingScopeFilters filters, ThreadLocalLoggingScopeFactory loggingScopeFactory) {
		this.sink = sink;
		this.filters = filters;
        this.loggingScopeFactory = loggingScopeFactory;
    }

	public void init(FilterConfig filterConfig) {
	}

	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		if(servletRequest instanceof HttpServletRequest) {
			HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

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
				filterChain.doFilter(httpServletRequest, servletResponse);
			} finally {
				HttpServletResponse response = (HttpServletResponse) servletResponse;
				if(filter.getHttpStatusFailurePredicate().test(response.getStatus())) {
					// was there an error response
					sink.write(scope);
				} else if(scope.isLogLevelFailure()) {
					// was there some dangerous error message?
					sink.write(scope);
				}
				loggingScopeFactory.closeScope(scope);
			}
		} else {
			filterChain.doFilter(servletRequest, servletResponse);
		}
	}

	public void destroy() {
	}

}
