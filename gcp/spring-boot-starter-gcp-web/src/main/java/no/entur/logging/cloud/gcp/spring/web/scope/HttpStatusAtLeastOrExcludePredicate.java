package no.entur.logging.cloud.gcp.spring.web.scope;

import java.util.Set;


public class HttpStatusAtLeastOrExcludePredicate extends HttpStatusAtLeastPredicate {

    protected final Set<Integer> exclude;

    public HttpStatusAtLeastOrExcludePredicate(int limit, Set<Integer> exclude) {
        super(limit);

        this.exclude = exclude;
    }

    @Override
    public boolean test(int statusCode) {
        return super.test(statusCode) && !exclude.contains(statusCode);
    }
}
