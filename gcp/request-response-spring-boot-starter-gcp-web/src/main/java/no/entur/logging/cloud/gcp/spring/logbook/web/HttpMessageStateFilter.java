package no.entur.logging.cloud.gcp.spring.logbook.web;

import jakarta.servlet.*;
import no.entur.logging.cloud.logbook.async.state.DefaultHttpMessageStateSupplier;

import java.io.IOException;

public class HttpMessageStateFilter implements Filter {

	public static final String HTTP_MESSAGE_STATE = HttpMessageStateFilter.class.getName()+":HTTP_MESSAGE_STATE";

	public void init(FilterConfig filterConfig) {
	}

	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		try {
			servletRequest.setAttribute(HTTP_MESSAGE_STATE, new DefaultHttpMessageStateSupplier());

			filterChain.doFilter(servletRequest, servletResponse);
		} finally {
			servletRequest.removeAttribute(HTTP_MESSAGE_STATE);
		}
	}

	public void destroy() {
	}

}
