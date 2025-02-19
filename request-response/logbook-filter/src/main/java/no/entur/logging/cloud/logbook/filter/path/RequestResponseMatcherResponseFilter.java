package no.entur.logging.cloud.logbook.filter.path;

import java.io.IOException;

import no.entur.logging.cloud.logbook.filter.path.strategy.RequestHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.ForwardingHttpResponse;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.ResponseFilter;

/**
 *
 * This filter only works with strategies like {@link no.entur.logging.cloud.logbook.filter.path.strategy.MatchPathStrategy}
 * which insert a {@link RequestHttpResponse} as delegate for {@link HttpResponse} of subclass {@link ForwardingHttpResponse}.
 */

public class RequestResponseMatcherResponseFilter implements ResponseFilter {

	private final static Logger log = LoggerFactory.getLogger(RequestResponseMatcherResponseFilter.class);

	public static PathPrefixRequestResponseMatcherResponseFilterBuilder newPathPrefixBuilder() {
		return new PathPrefixRequestResponseMatcherResponseFilterBuilder();
	}

	private final RequestResponseFilterCollection filter;

	public RequestResponseMatcherResponseFilter(RequestResponseFilterCollection filter) {
		this.filter = filter;
	}
	
	@Override
	public HttpResponse filter(HttpResponse response) {
		RequestHttpResponse target = null;
		
		HttpResponse next = response;
		do {
			if(next instanceof RequestHttpResponse) {
				target = (RequestHttpResponse)next;
				break;
			}
			if(next instanceof ForwardingHttpResponse) {
				ForwardingHttpResponse f = (ForwardingHttpResponse)next;
				next = f.delegate();
				continue;
			}
			break;
		} while(next != null);
		
		if(target == null) {
			return response;
		}

		try {
			String body = response.getBodyAsString();
			if(body == null || body.length() == 0) {
				return response;
			}

			BodyFilter f = filter.getBodyFilter(target.getRequest(), response, body.length());
		
			if(f != null) {
				String filtered = f.filter(response.getContentType(), body);
				if(filtered != null) {
					return new BodyReplacementHttpResponse(response, filtered);
				}
			}
		} catch (IOException e) {
			log.warn("Problem filtering response body", e);
		}

		return response;
	}

}
