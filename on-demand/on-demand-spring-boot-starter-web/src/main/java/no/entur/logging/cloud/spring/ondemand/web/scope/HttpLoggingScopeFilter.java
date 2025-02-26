package no.entur.logging.cloud.spring.ondemand.web.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.time.Duration;
import java.util.Enumeration;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

public class HttpLoggingScopeFilter {

    private Predicate<ILoggingEvent> queuePredicate;
    private Predicate<ILoggingEvent> ignorePredicate;

    private Predicate<ILoggingEvent> troubleshootQueuePredicate;

    private Predicate<ILoggingEvent> troubleshootIgnorePredicate;

    private Predicate<ILoggingEvent> logLevelFailurePredicate;
    private IntPredicate httpStatusFailurePredicate;

    private Predicate<Enumeration<String>> httpHeaderPresentPredicate;

    private LongPredicate failureDuration;

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

    public void setFailureDuration(Duration before, Duration after) {
        boolean hasBefore = before != null;
        boolean hasAfter = after != null;

        if(hasBefore && hasAfter) {
            long beforeMillis = before.toMillis();
            long afterMillis = after.toMillis();

            if(beforeMillis > afterMillis) {
                //  assume interval [after, before] => failure
                failureDuration = (time) -> afterMillis < time && time < beforeMillis;
            } else {
                //  assume intervals [0, before] or [after, infinite] => failure
                failureDuration = (time) -> time < beforeMillis || afterMillis < time;
            }
        } else if(hasBefore) {
            long beforeMillis = before.toMillis();
            failureDuration = (time) -> time < beforeMillis;
        } else if(hasAfter) {
            long afterMillis = after.toMillis();
            failureDuration = (time) -> time > afterMillis;
        }
    }

    public boolean isFailureForDuration(long duration) {
        System.out.println("Check " + duration + " " + (failureDuration != null && failureDuration.test(duration)));
        return failureDuration != null && failureDuration.test(duration);
    }

}
