package no.entur.logging.cloud.gcp.trace.spring.web;

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
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * Filter in support of call tracing. Get correlation id, trace, spandId, from request or create a new one. Add a request id.
 * The value is added to the {@linkplain HttpServletRequest} for further processing. 
 *
 * @see <a href="https://cloud.google.com/logging/docs/structured-logging">structured-logging</a>
 */

public class GcpCorrelationIdFilter implements Filter {

	public static final String REQUEST_ID_MDC_KEY = "requestId";
	public static final String CORRELATION_ID_MDC_KEY = "correlationId";

	// The span ID is expected to be a 16-character, hexadecimal encoding of an 8-byte array and should not be zero. It should be unique within the trace and should, ideally, be generated in a manner that is uniformly random.
	public static final String SPAN_ID_MDC_KEY = "spanId";

	// Note: If not writing to stdout or stderr, the value of this field should be formatted as projects/[PROJECT-ID]/traces/[TRACE-ID],
	// so it can be used by the Logs Explorer and the Trace Viewer to group log entries and display them in line
	// with traces.

	public static final String TRACE_MDC_KEY = "trace";

	public static final String CORRELATION_ID_HTTP_HEADER = "X-Correlation-Id";

	public static final String REQUEST_ID_HTTP_REQUEST_KEY = GcpCorrelationIdFilter.class.getName() + ".request";
	public static final String CORRELATION_ID_HTTP_REQUEST_KEY = GcpCorrelationIdFilter.class.getName() + ".correlationId";
	public static final String TRACE_HTTP_REQUEST_KEY = GcpCorrelationIdFilter.class.getName() + ".trace";
	public static final String SPAN_ID_HTTP_REQUEST_KEY = GcpCorrelationIdFilter.class.getName() + ".spanId";

	public static void setCorrelationId(ServletRequest request, String value) {
		request.setAttribute(CORRELATION_ID_HTTP_REQUEST_KEY, value);
	}

	public static void setTrace(ServletRequest request, String value) {
		request.setAttribute(TRACE_HTTP_REQUEST_KEY, value);
	}

	public static void setSpanId(ServletRequest request, String value) {
		request.setAttribute(SPAN_ID_HTTP_REQUEST_KEY, value);
	}

	public static String getCorrelationId(ServletRequest request) {
		return (String) request.getAttribute(CORRELATION_ID_HTTP_REQUEST_KEY);
	}

	public static void setRequestId(ServletRequest request, String value) {
		request.setAttribute(REQUEST_ID_HTTP_REQUEST_KEY, value);
	}

	public static String getRequestId(ServletRequest request) {
		return (String) request.getAttribute(REQUEST_ID_HTTP_REQUEST_KEY);
	}

	public static String getSpanId(ServletRequest request) {
		return (String) request.getAttribute(SPAN_ID_HTTP_REQUEST_KEY);
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
				correlationId = GcpCorrelationIdMdcSupportBuilder.sanitize(inputValue);
			}

			setCorrelationId(request, correlationId);
			setTrace(request, correlationId);

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

		String spanId = getSpanId(servletRequest);
		if(spanId == null) {
			//  16-character, hexadecimal encoding of an 8-byte array
			ThreadLocalRandom current = ThreadLocalRandom.current();
			long random = current.nextLong();
			spanId = Long.toHexString(random);
			setSpanId(servletRequest, spanId);
		}

		MDC.put(REQUEST_ID_MDC_KEY, requestId);
		MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

		MDC.put(TRACE_MDC_KEY, requestId);
		MDC.put(SPAN_ID_MDC_KEY, spanId);
		try {
			chain.doFilter(servletRequest, servletResponse);
		} finally {
			MDC.remove(REQUEST_ID_MDC_KEY);
			MDC.remove(CORRELATION_ID_MDC_KEY);

			MDC.remove(TRACE_MDC_KEY);
			MDC.remove(SPAN_ID_MDC_KEY);
		}

	}

}
