package no.entur.logging.cloud.azure.spring.ondemand.grpc.lognet.scope;

import io.grpc.Metadata;

import java.util.Enumeration;
import java.util.Set;
import java.util.function.Predicate;


public class GrpcHeaderPresentPredicate implements Predicate<Metadata> {

    protected final Set<String> names;

    public GrpcHeaderPresentPredicate(Set<String> names) {
        this.names = names;
    }

    @Override
    public boolean test(Metadata metadata) {
        for(String key : metadata.keys()) {
            if(names.contains(key)) {
                return true;
            }
        }
        return false;
    }
}
