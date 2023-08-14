package no.entur.logging.cloud.gcp.spring.web;

import java.util.function.Predicate;

@FunctionalInterface
public interface OndemandLoggerPredicate extends Predicate<String> {
}
