package no.entur.logging.cloud.gcp.spring.web;

import ch.qos.logback.classic.spi.ILoggingEvent;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;

import java.io.IOException;
import java.util.function.Predicate;

public class OndemandFilter implements Filter {

	private final LoggingScopeAsyncAppender appender;
	private final Predicate<ServletResponse> responsePredicate;

	public OndemandFilter(LoggingScopeAsyncAppender appender, Predicate<ServletResponse> responsePredicate) {
		this.appender = appender;
		this.responsePredicate = responsePredicate;
	}

	public void init(FilterConfig filterConfig) {
	}

	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		try {
			appender.openScope();

			filterChain.doFilter(servletRequest, servletResponse);
		} finally {
			if(responsePredicate.test(servletResponse)) {
				appender.flushScope();
			}
			appender.closeScope();
		}
	}

	public void destroy() {
	}
	
}
