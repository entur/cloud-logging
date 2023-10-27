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
import java.util.Enumeration;
import java.util.concurrent.ConcurrentLinkedQueue;
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
			HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

			HttpLoggingScopeFilter filter = filters.getScope(httpServletRequest);

			Predicate<ILoggingEvent> queuePredicate = filter.getQueuePredicate();
			Predicate<ILoggingEvent> ignorePredicate = filter.getIgnorePredicate();

			Predicate<Enumeration<String>> httpHeaderPresentPredicate = filter.getHttpHeaderPresentPredicate();
			if(httpHeaderPresentPredicate.test(httpServletRequest.getHeaderNames())) {
				queuePredicate = filter.getTroubleshootQueuePredicate();
				ignorePredicate = filter.getTroubleshootIgnorePredicate();
			}

			LoggingScopeFactory loggingScopeFactory = appender.getLoggingScopeFactory();
			try {
				loggingScopeFactory.openScope(queuePredicate, ignorePredicate);

				filterChain.doFilter(httpServletRequest, servletResponse);
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
