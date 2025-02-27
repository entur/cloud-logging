package no.entur.logging.cloud.spring.ondemand.web.scope;

import java.util.List;
import java.util.function.IntPredicate;


public class HttpStatusAtLeastOrNotPredicate extends HttpStatusAtLeastPredicate {

    protected final IntPredicate exclude;
    protected final IntPredicate include;

    public HttpStatusAtLeastOrNotPredicate(int limit, List<Integer> include, List<Integer> exclude) {
        super(limit);

        this.include = include.isEmpty() ? (v) -> false : new HttpStatusEqualToPredicate(include);
        this.exclude = exclude.isEmpty() ? (v) -> false : new HttpStatusNotEqualToPredicate(exclude);
    }

    @Override
    public boolean test(int statusCode) {
        if(super.test(statusCode)) {
            // status code is equal or higher than limit
            return exclude.test(statusCode);
        } else {
            // status code is lower than limit
            return include.test(statusCode);
        }
    }
}
