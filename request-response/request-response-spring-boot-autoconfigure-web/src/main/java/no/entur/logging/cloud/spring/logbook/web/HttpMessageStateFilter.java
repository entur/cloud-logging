package no.entur.logging.cloud.spring.logbook.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import no.entur.logging.cloud.logbook.ondemand.state.DefaultHttpMessageStateSupplier;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageStateSupplier;
import no.entur.logging.cloud.spring.ondemand.web.scope.HttpLoggingScopeFilter;

import java.io.IOException;

public class HttpMessageStateFilter implements Filter {

	public static final String HTTP_MESSAGE_STATE = HttpMessageStateFilter.class.getName()+":HTTP_MESSAGE_STATE";

	public void init(FilterConfig filterConfig) {
	}

	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpMessageStateSupplier supplier = getHttpMessageStateSupplier(servletRequest);
		if(supplier == null) {
			servletRequest.setAttribute(HTTP_MESSAGE_STATE, new DefaultHttpMessageStateSupplier());
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}

	private HttpMessageStateSupplier getHttpMessageStateSupplier(ServletRequest servletRequest) {
		return (HttpMessageStateSupplier) servletRequest.getAttribute(HTTP_MESSAGE_STATE);
	}

	public void destroy() {
	}

}
