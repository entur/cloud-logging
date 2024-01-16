package no.entur.logging.cloud.gcp.spring.ondemand.web.scope;

import java.util.List;
import java.util.function.IntPredicate;


public class HttpStatusEqualToPredicate implements IntPredicate {

    protected final boolean[] matchers;

    public HttpStatusEqualToPredicate(List<Integer> codes) {
        Integer max = codes.get(0);
        for(int i = 1; i < codes.size(); i++) {
            if(max < codes.get(i)) {
                max = codes.get(i);
            }
        }

        matchers = new boolean[max + 1];
        for (Integer code : codes) {
            matchers[code] = true;
        }
    }

    @Override
    public boolean test(int i) {
        if(i >= matchers.length) {
            return false;
        }
        return matchers[i];
    }
}
