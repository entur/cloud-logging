package no.entur.logging.cloud.logbook.filter.path.matcher;

import no.entur.logging.cloud.logbook.filter.path.RequestFilterCollection;
import no.entur.logging.cloud.logbook.filter.path.RequestResponseFilterCollection;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Origin;

import java.util.List;

public class MatcherPathFilterCollection implements RequestFilterCollection, RequestResponseFilterCollection {

	protected final PathFilterMatcher[] paths;

	public MatcherPathFilterCollection(List<PathFilterMatcher> requests) {
		this(requests.toArray(new PathFilterMatcher[requests.size()]));
	}

	public MatcherPathFilterCollection(PathFilterMatcher[] requests) {
		this.paths = requests;
	}

	public BodyFilter getBodyFilter(String path, Origin origin, int size) {
		for(PathFilterMatcher matcher : paths) {
			if(matcher.matches(path)) {
				return matcher.getBodyFilter(origin, size);
			}
		}
		return null;
	}

	protected PathFilterMatcher[] getFilters() {
		return paths;
	}

	@Override
	public BodyFilter getBodyFilter(HttpRequest request, int size) {
		return getBodyFilter(request.getPath(), request.getOrigin(), size);
	}

	@Override
	public BodyFilter getBodyFilter(HttpRequest request, HttpResponse httpResponse, int size) {
		return getBodyFilter(request.getPath(), httpResponse.getOrigin(), size);
	}
}
