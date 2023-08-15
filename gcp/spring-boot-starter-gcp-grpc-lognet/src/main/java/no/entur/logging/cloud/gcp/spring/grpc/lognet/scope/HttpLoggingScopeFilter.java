package no.entur.logging.cloud.gcp.spring.grpc.lognet.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.function.IntPredicate;
import java.util.function.Predicate;

public class HttpLoggingScopeFilter {

    private Predicate<ILoggingEvent> queuePredicate;
    private Predicate<ILoggingEvent> ignorePredicate;

    private Predicate<ILoggingEvent> logLevelFailurePredicate;
    private IntPredicate httpStatusFailurePredicate;

    public IntPredicate getHttpStatusFailurePredicate() {
        return httpStatusFailurePredicate;
    }

    public void setHttpStatusFailurePredicate(IntPredicate httpStatusFailurePredicate) {
        this.httpStatusFailurePredicate = httpStatusFailurePredicate;
    }

    public Predicate<ILoggingEvent> getQueuePredicate() {
        return queuePredicate;
    }

    public void setQueuePredicate(Predicate<ILoggingEvent> queuePredicate) {
        this.queuePredicate = queuePredicate;
    }

    public Predicate<ILoggingEvent> getIgnorePredicate() {
        return ignorePredicate;
    }

    public void setIgnorePredicate(Predicate<ILoggingEvent> ignorePredicate) {
        this.ignorePredicate = ignorePredicate;
    }

    public Predicate<ILoggingEvent> getLogLevelFailurePredicate() {
        return logLevelFailurePredicate;
    }

    public void setLogLevelFailurePredicate(Predicate<ILoggingEvent> logLevelFailurePredicate) {
        this.logLevelFailurePredicate = logLevelFailurePredicate;
    }
}
