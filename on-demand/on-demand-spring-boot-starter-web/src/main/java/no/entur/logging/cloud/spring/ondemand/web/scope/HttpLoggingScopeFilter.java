package no.entur.logging.cloud.spring.ondemand.web.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.Enumeration;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

public class HttpLoggingScopeFilter {

    private Predicate<ILoggingEvent> queuePredicate;
    private Predicate<ILoggingEvent> ignorePredicate;

    private Predicate<ILoggingEvent> troubleshootQueuePredicate;

    private Predicate<ILoggingEvent> troubleshootIgnorePredicate;

    private Predicate<ILoggingEvent> logLevelFailurePredicate;
    private IntPredicate httpStatusFailurePredicate;

    private Predicate<Enumeration<String>> httpHeaderPresentPredicate;
    private long failureDuration = -1L; // in milliseconds

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

    public Predicate<Enumeration<String>> getHttpHeaderPresentPredicate() {
        return httpHeaderPresentPredicate;
    }

    public void setHttpHeaderPresentPredicate(Predicate<Enumeration<String>> httpHeaderPresentPredicate) {
        this.httpHeaderPresentPredicate = httpHeaderPresentPredicate;
    }

    public Predicate<ILoggingEvent> getTroubleshootIgnorePredicate() {
        return troubleshootIgnorePredicate;
    }

    public void setTroubleshootIgnorePredicate(Predicate<ILoggingEvent> troubleshootIgnorePredicate) {
        this.troubleshootIgnorePredicate = troubleshootIgnorePredicate;
    }

    public void setTroubleshootQueuePredicate(Predicate<ILoggingEvent> troubleshootQueuePredicate) {
        this.troubleshootQueuePredicate = troubleshootQueuePredicate;
    }

    public Predicate<ILoggingEvent> getTroubleshootQueuePredicate() {
        return troubleshootQueuePredicate;
    }

    public void setFailureDuration(long failureDuration) {
        this.failureDuration = failureDuration;
    }

    public long getFailureDuration() {
        return failureDuration;
    }

    public boolean hasFailureDuration() {
        return failureDuration != -1L;
    }
}
