package no.entur.logging.cloud.logbook.filter.path.strategy;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Strategy;

public class MatchPathStrategy implements Strategy {

	private final List<Predicate<String>> requestsMatchers;
	private final List<Predicate<String>> responseMatchers;

    public MatchPathStrategy(List<Predicate<String>> requestsMatchers, List<Predicate<String>> responseMatchers) {
        this.requestsMatchers = requestsMatchers;
        this.responseMatchers = responseMatchers;
    }

    public HttpRequest process(final HttpRequest request) throws IOException {
		if(!includeRequest(request)) {
			return request.withoutBody();
		}
		return request.withBody();
    }
	
    public HttpResponse process(final HttpRequest request, final HttpResponse response) throws IOException {
		if(!includeResponse(request, response)) {
			return response.withoutBody();
		}
        return new DelegateRequestHttpResponse(request, response.withBody(), request.getPath());
    }

	protected boolean includeRequest(HttpRequest request) {
		String path = request.getPath();
		if(path != null) {
			for(Predicate<String> m : requestsMatchers) {
				if(m.test(path)) {
					return true;
				}
			}
		}
		return false;
	}

	protected boolean includeResponse(HttpRequest request, HttpResponse response) {
		String path = request.getPath();
		if(path != null) {
			for(Predicate<String> m : responseMatchers) {
				if(m.test(path)) {
					return true;
				}
			}
		}
		return false;
	}
}
