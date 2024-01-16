package no.entur.logging.cloud.gcp.spring.ondemand.web.scope;

import java.util.List;
import java.util.function.IntPredicate;


public class HttpStatusNotEqualToPredicate implements IntPredicate {

    protected final boolean[] matchers;

    public HttpStatusNotEqualToPredicate(List<Integer> codes) {
        Integer max = codes.get(0);
        for(int i = 1; i < codes.size(); i++) {
            if(max < codes.get(i)) {
                max = codes.get(i);
            }
        }

        matchers = new boolean[max + 1];
        for(int i = 0; i < matchers.length; i++) {
            matchers[i] = true;
        }

        for (Integer code : codes) {
            matchers[code] = false;
        }
    }

    @Override
    public boolean test(int i) {
        if(i >= matchers.length) {
            return true;
        }
        return matchers[i];
    }
}
