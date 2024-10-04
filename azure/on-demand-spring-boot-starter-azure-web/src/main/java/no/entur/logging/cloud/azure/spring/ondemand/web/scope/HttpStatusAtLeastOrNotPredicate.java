package no.entur.logging.cloud.azure.spring.ondemand.web.scope;

import java.util.List;
import java.util.Set;


public class HttpStatusAtLeastOrNotPredicate extends HttpStatusAtLeastPredicate {

    protected final HttpStatusNotEqualToPredicate exclude;
    protected final HttpStatusEqualToPredicate include;

    public HttpStatusAtLeastOrNotPredicate(int limit, List<Integer> include, List<Integer> exclude) {
        super(limit);

        this.exclude = new HttpStatusNotEqualToPredicate(exclude);
        this.include = new HttpStatusEqualToPredicate(include);
    }

    @Override
    public boolean test(int statusCode) {
        if(super.test(statusCode)) {
            // status code is equal or higher
            return exclude.test(statusCode);
        } else {
            // status code is lower
            return include.test(statusCode);
        }
    }
}
