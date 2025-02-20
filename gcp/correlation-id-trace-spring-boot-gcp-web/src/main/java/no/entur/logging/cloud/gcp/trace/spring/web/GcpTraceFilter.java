package no.entur.logging.cloud.gcp.trace.spring.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import no.entur.logging.cloud.trace.spring.web.CorrelationIdFilter;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * Filter which supports a few Stackdriver-specific logging fields
 */

public class GcpTraceFilter implements Filter {

	// The span ID is expected to be a 16-character, hexadecimal encoding of an 8-byte array and should not be zero. It should be unique within the trace and should, ideally, be generated in a manner that is uniformly random.
	public static final String SPAN_ID_MDC_KEY = "spanId";

	// Note: If not writing to stdout or stderr, the value of this field should be formatted as projects/[PROJECT-ID]/traces/[TRACE-ID],
	// so it can be used by the Logs Explorer and the Trace Viewer to group log entries and display them in line
	// with traces.

	public static final String TRACE_MDC_KEY = "trace";

	public static final String TRACE_HTTP_REQUEST_KEY = GcpTraceFilter.class.getName() + ".trace";
	public static final String SPAN_ID_HTTP_REQUEST_KEY = GcpTraceFilter.class.getName() + ".spanId";

	public static void setTrace(ServletRequest request, String value) {
		request.setAttribute(TRACE_HTTP_REQUEST_KEY, value);
	}

	public static void setSpanId(ServletRequest request, String value) {
		request.setAttribute(SPAN_ID_HTTP_REQUEST_KEY, value);
	}

	public static String getSpanId(ServletRequest request) {
		return (String) request.getAttribute(SPAN_ID_HTTP_REQUEST_KEY);
	}

	public static String getTrace(ServletRequest request) {
		return (String) request.getAttribute(TRACE_HTTP_REQUEST_KEY);
	}


	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
		String trace = getTrace(servletRequest);
		if(trace == null) {
			// copy from correlation-id, if possible
			String correlationId = CorrelationIdFilter.getCorrelationId(servletRequest);
			if(correlationId != null) {
				trace = "projects/prsnlstn/traces/" + correlationId;
			} else {
				trace = "projects/prsnlstn/traces/" + UUID.randomUUID().toString();
			}
			setTrace(servletRequest, trace);
		}

		String spanId = getSpanId(servletRequest);
		if(spanId == null) {
			//  16-character, hexadecimal encoding of an 8-byte array
			ThreadLocalRandom current = ThreadLocalRandom.current();
			long random = current.nextLong();
			spanId = Long.toHexString(random);
			setSpanId(servletRequest, spanId);
		}

		MDC.put(TRACE_MDC_KEY, trace);
		MDC.put(SPAN_ID_MDC_KEY, spanId);
		try {
			chain.doFilter(servletRequest, servletResponse);
		} finally {
			MDC.remove(TRACE_MDC_KEY);
			MDC.remove(SPAN_ID_MDC_KEY);
		}

	}

}
