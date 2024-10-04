package no.entur.logging.cloud.azure.spring.ondemand.web.scope;

import java.util.Enumeration;
import java.util.Set;
import java.util.function.Predicate;


public class HttpHeaderPresentPredicate implements Predicate<Enumeration<String>> {

    protected final Set<String> names;

    public HttpHeaderPresentPredicate(Set<String> names) {
        this.names = names;
    }

    @Override
    public boolean test(Enumeration<String> stringEnumeration) {
        while(stringEnumeration.hasMoreElements()) {
            if(names.contains(stringEnumeration.nextElement())) {
                return true;
            }
        }
        return false;
    }
}
