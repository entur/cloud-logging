package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.AsyncAppender;
import no.entur.logging.cloud.appender.MdcAsyncAppender;
import no.entur.logging.cloud.appender.scope.*;
import org.slf4j.MDC;
import org.slf4j.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScopeAsyncAppender extends MdcAsyncAppender implements LoggingScopeSink {

    private List<LoggingScopeProvider> scopeProviders = new ArrayList<>();

    public void addScopeProvider(LoggingScopeProvider scopeProvider) {
        this.scopeProviders.add(scopeProvider);
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (isQueueBelowDiscardingThreshold() && isDiscardable(eventObject)) {
            return;
        }
        preprocess(eventObject);

        LoggingScope scope = getCurrentScope();
        if(scope == null || !scope.append(eventObject)) {
            postProcess(eventObject);
            put(eventObject);
        }
    }

    public LoggingScope getCurrentScope() {
        for (LoggingScopeProvider loggingScopeFactory : scopeProviders) {
            LoggingScope scope = loggingScopeFactory.getCurrentScope();
            if(scope != null) {
                return scope;
            }
        }
        return null;
    }

    public void write(LoggingScope scope) {
        ConcurrentLinkedQueue<ILoggingEvent> events = scope.getEvents();
        for (ILoggingEvent eventObject : events) {
            postProcess(eventObject);
            put(eventObject);
        }
    }

    private void postProcess(ILoggingEvent eventObject) {
        List<Marker> markerList = eventObject.getMarkerList();
        if(markerList != null && !markerList.isEmpty()) {
            for (Marker marker : markerList) {
                if(marker instanceof LoggingScopePostProcessing postProcessing) {
                    postProcessing.performPostProcessing();
                }
            }
        }
    }

    public List<LoggingScopeProvider> getScopeProviders() {
        return scopeProviders;
    }
}
