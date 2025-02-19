package no.entur.logging.cloud.trace.spring.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
/**
 * 
 * Filter in support of call tracing. Get correlation id from request or create a new one. Add a request id.
 * The value is added to the {@linkplain HttpServletRequest} for further processing. 
 * 
 */

public class CorrelationIdFilter implements Filter {

	public static final String REQUEST_ID_MDC_KEY = "requestId";
	public static final String CORRELATION_ID_MDC_KEY = "correlationId";

	public static final String CORRELATION_ID_HTTP_HEADER = "X-Correlation-Id";

	public static final String REQUEST_ID_HTTP_REQUEST_KEY = CorrelationIdFilter.class.getName() + ".request";
	public static final String CORRELACTION_ID_HTTP_REQUEST_KEY = CorrelationIdFilter.class.getName() + ".correlationId";

	public static void setCorrelationId(ServletRequest request, String value) {
		request.setAttribute(CORRELACTION_ID_HTTP_REQUEST_KEY, value);
	}

	public static String getCorrelationId(ServletRequest request) {
		return (String) request.getAttribute(CORRELACTION_ID_HTTP_REQUEST_KEY);
	}

	public static void setRequestId(ServletRequest request, String value) {
		request.setAttribute(REQUEST_ID_HTTP_REQUEST_KEY, value);
	}

	public static String getRequestId(ServletRequest request) {
		return (String) request.getAttribute(REQUEST_ID_HTTP_REQUEST_KEY);
	}

	public List<String> getHeaders() {
		return Arrays.asList(CORRELATION_ID_MDC_KEY);
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {


		String correlationId = getCorrelationId(servletRequest);
		if(correlationId == null) {
			// spawn a correlation-id
			HttpServletRequest request = (HttpServletRequest) servletRequest;
			String inputValue = request.getHeader(CORRELATION_ID_HTTP_HEADER);

			if (inputValue == null) {
				correlationId = UUID.randomUUID().toString();
			} else {
				correlationId = CorrelationIdMdcSupportBuilder.sanitize(inputValue);
			}

			setCorrelationId(request, correlationId);

			// set correlation-id on response for tracking
			HttpServletResponse response = (HttpServletResponse) servletResponse;
			response.setHeader(CORRELATION_ID_HTTP_HEADER, correlationId);
		}

		// also add a request id for cases when the same service is invoked multiple times with
		// the same correlation-id
		String requestId = getRequestId(servletRequest);
		if(requestId == null) {
			requestId = UUID.randomUUID().toString();
			setRequestId(servletRequest, requestId);
		}

		MDC.put(REQUEST_ID_MDC_KEY, requestId);
		MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
		try {
			chain.doFilter(servletRequest, servletResponse);
		} finally {
			MDC.remove(REQUEST_ID_MDC_KEY);
			MDC.remove(CORRELATION_ID_MDC_KEY);
		}

	}

}
