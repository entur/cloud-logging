package no.entur.logging.cloud.gcp.spring.grpc.lognet.scope;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.List;

public class GrpcLoggingScopeFilters {

    private static class Filter {
        private RequestMatcher requestMatcher;
        private GrpcLoggingScopeFilter scope;

        public Filter(RequestMatcher requestMatcher, GrpcLoggingScopeFilter scope) {
            this.requestMatcher = requestMatcher;
            this.scope = scope;
        }
    }

    private GrpcLoggingScopeFilter defaultFilter;

    private List<Filter> filters = new ArrayList<>();

    public void addFilter(RequestMatcher pathMatcher, GrpcLoggingScopeFilter filter) {
        filters.add(new Filter(pathMatcher, filter));
    }

    public void setDefaultFilter(GrpcLoggingScopeFilter defaultFilter) {
        this.defaultFilter = defaultFilter;
    }

    public GrpcLoggingScopeFilter getScope(HttpServletRequest httpServletRequest) {
        for (Filter filter : filters) {
            if(filter.requestMatcher.matches(httpServletRequest)) {
                return filter.scope;
            }
        }
        return defaultFilter;
    }

}
