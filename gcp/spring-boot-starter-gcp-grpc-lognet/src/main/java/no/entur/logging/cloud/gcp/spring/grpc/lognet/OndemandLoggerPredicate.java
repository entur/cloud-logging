package no.entur.logging.cloud.gcp.spring.grpc.lognet;

import java.util.function.Predicate;

@FunctionalInterface
public interface OndemandLoggerPredicate extends Predicate<String> {
}
