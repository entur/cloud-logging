package no.entur.logging.cloud.logbook.filter.path;

import no.entur.logging.cloud.logbook.filter.path.matcher.MatcherPathFilterCollection;
import no.entur.logging.cloud.logbook.filter.path.matcher.PathFilterMatcher;
import no.entur.logging.cloud.logbook.filter.path.matcher.SimplePathFilterMatcher;
import org.zalando.logbook.BodyFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathPrefixRequestMatcherRequestFilter {

    private Map<String, BodyFilter> pathPrefixFilters = new HashMap<>();

    public PathPrefixRequestMatcherRequestFilter withPathPrefixFilter(String path, BodyFilter bodyFilter) {
        pathPrefixFilters.put(path, bodyFilter);
        return this;
    }

    public RequestMatcherRequestFilter build() {
        List<PathFilterMatcher> matchers = new ArrayList<>(pathPrefixFilters.size());

        for (Map.Entry<String, BodyFilter> entry : pathPrefixFilters.entrySet()) {

            SimplePathFilterMatcher matcher = new SimplePathFilterMatcher( (path) -> path != null && path.startsWith(entry.getKey()), entry.getValue());

            matchers.add(matcher);
        }
        MatcherPathFilterCollection collection = new MatcherPathFilterCollection(matchers);
        return new RequestMatcherRequestFilter(collection);
    }

}
