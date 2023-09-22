package no.entur.logging.cloud.gcp.spring.web;

import ch.qos.logback.classic.spi.ILoggingEvent;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;
import no.entur.logging.cloud.appender.scope.LoggingScopeFactory;
import no.entur.logging.cloud.gcp.spring.web.scope.HttpLoggingScopeFilter;
import no.entur.logging.cloud.gcp.spring.web.scope.HttpLoggingScopeFilters;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class OndemandFilter implements Filter {

	private final LoggingScopeAsyncAppender appender;
	private final HttpLoggingScopeFilters filters;

	public OndemandFilter(LoggingScopeAsyncAppender appender, HttpLoggingScopeFilters filters) {
		this.appender = appender;
		this.filters = filters;
	}

	public void init(FilterConfig filterConfig) {
	}

	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		if(servletRequest instanceof HttpServletRequest) {
			HttpLoggingScopeFilter filter = filters.getScope((HttpServletRequest) servletRequest);

			LoggingScopeFactory loggingScopeFactory = appender.getLoggingScopeFactory();
			try {
				loggingScopeFactory.openScope(filter.getQueuePredicate(), filter.getIgnorePredicate());

				filterChain.doFilter(servletRequest, servletResponse);
			} finally {
				LoggingScope scope = loggingScopeFactory.getScope();
				if(scope != null) {
					HttpServletResponse response = (HttpServletResponse) servletResponse;
					if(filter.getHttpStatusFailurePredicate().test(response.getStatus())) {
						// was there an error response
						appender.flushScope();
					} else {
						// was there some dangerous error message?
						Predicate<ILoggingEvent> logLevelFailurePredicate = filter.getLogLevelFailurePredicate();
						ConcurrentLinkedQueue<ILoggingEvent> events = scope.getEvents();
						for (ILoggingEvent event : events) {
							if (logLevelFailurePredicate.test(event)) {
								appender.flushScope();
								break;
							}
						}
					}
					appender.closeScope();
				}
			}
		} else {
			filterChain.doFilter(servletRequest, servletResponse);
		}
	}

	public void destroy() {
	}

}
