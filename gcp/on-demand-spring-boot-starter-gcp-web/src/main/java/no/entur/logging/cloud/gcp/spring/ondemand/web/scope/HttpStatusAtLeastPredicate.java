package no.entur.logging.cloud.gcp.spring.ondemand.web.scope;

import java.util.function.IntPredicate;


public class HttpStatusAtLeastPredicate implements IntPredicate {

    protected final int limit;

    public HttpStatusAtLeastPredicate(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean test(int i) {
        return i >= limit;
    }
}
