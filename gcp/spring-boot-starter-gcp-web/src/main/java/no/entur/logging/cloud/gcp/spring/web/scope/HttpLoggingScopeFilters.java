package no.entur.logging.cloud.gcp.spring.web.scope;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.List;

public class HttpLoggingScopeFilters {

    private static class Filter {
        private RequestMatcher requestMatcher;
        private HttpLoggingScopeFilter scope;

        public Filter(RequestMatcher requestMatcher, HttpLoggingScopeFilter scope) {
            this.requestMatcher = requestMatcher;
            this.scope = scope;
        }
    }

    private HttpLoggingScopeFilter defaultFilter;

    private List<Filter> filters = new ArrayList<>();

    public void addFilter(RequestMatcher pathMatcher, HttpLoggingScopeFilter filter) {
        filters.add(new Filter(pathMatcher, filter));
    }

    public void setDefaultFilter(HttpLoggingScopeFilter defaultFilter) {
        this.defaultFilter = defaultFilter;
    }

    public HttpLoggingScopeFilter getScope(HttpServletRequest httpServletRequest) {
        for (Filter filter : filters) {
            if(filter.requestMatcher.matches(httpServletRequest)) {
                return filter.scope;
            }
        }
        return defaultFilter;
    }

}
