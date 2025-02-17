package no.entur.logging.cloud.spring.ondemand.grpc.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.grpc.Metadata;
import io.grpc.Status;

import java.util.function.Predicate;

public class GrpcLoggingScopeFilter {

    private Predicate<ILoggingEvent> queuePredicate;
    private Predicate<ILoggingEvent> ignorePredicate;

    private Predicate<ILoggingEvent> logLevelFailurePredicate;
    private Predicate<Status> grpcStatusPredicate;

    private Predicate<ILoggingEvent> troubleshootQueuePredicate;

    private Predicate<ILoggingEvent> troubleshootIgnorePredicate;

    private Predicate<Metadata> grpcHeaderPresentPredicate;

    public Predicate<Status> getGrpcStatusPredicate() {
        return grpcStatusPredicate;
    }
    private long failureDuration = -1L; // in milliseconds

    public void setGrpcStatusPredicate(Predicate<Status> grpcStatusPredicate) {
        this.grpcStatusPredicate = grpcStatusPredicate;
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

    public Predicate<ILoggingEvent> getTroubleshootIgnorePredicate() {
        return troubleshootIgnorePredicate;
    }

    public Predicate<ILoggingEvent> getTroubleshootQueuePredicate() {
        return troubleshootQueuePredicate;
    }

    public void setTroubleshootIgnorePredicate(Predicate<ILoggingEvent> troubleshootIgnorePredicate) {
        this.troubleshootIgnorePredicate = troubleshootIgnorePredicate;
    }

    public void setTroubleshootQueuePredicate(Predicate<ILoggingEvent> troubleshootQueuePredicate) {
        this.troubleshootQueuePredicate = troubleshootQueuePredicate;
    }

    public Predicate<Metadata> getGrpcHeaderPresentPredicate() {
        return grpcHeaderPresentPredicate;
    }

    public void setGrpcHeaderPresentPredicate(Predicate<Metadata> grpcHeaderPresentPredicate) {
        this.grpcHeaderPresentPredicate = grpcHeaderPresentPredicate;
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
