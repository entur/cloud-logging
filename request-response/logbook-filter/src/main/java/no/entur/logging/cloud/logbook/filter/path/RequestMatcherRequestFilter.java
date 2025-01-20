package no.entur.logging.cloud.logbook.filter.path;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.RequestFilter;

public class RequestMatcherRequestFilter implements RequestFilter {

	private final static Logger log = LoggerFactory.getLogger(RequestMatcherRequestFilter.class);

	public static PathPrefixRequestMatcherRequestFilter newPathPrefixBuilder() {
		return new PathPrefixRequestMatcherRequestFilter();
	}

	private final RequestFilterCollection filter;
	
	public RequestMatcherRequestFilter(RequestFilterCollection filter) {
		this.filter = filter;
	}
	
	@Override
	public HttpRequest filter(HttpRequest request) {
		try {
			String body = request.getBodyAsString();

			if(body == null || body.length() == 0) {
				return request;
			}

			BodyFilter f = filter.getBodyFilter(request, body.length());

			if(f != null) {
				String filtered = f.filter(request.getContentType(), body);
				if(filtered != null) {
					return new BodyReplacementHttpRequest(request, filtered);
				}
			}
		} catch (IOException e) {
			log.warn("Problem filtering request body", e);
		}

		return request;
	}

}
