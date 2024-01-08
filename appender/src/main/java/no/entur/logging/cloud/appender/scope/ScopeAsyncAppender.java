package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.AsyncAppender;
import no.entur.logging.cloud.appender.scope.*;
import org.slf4j.MDC;
import org.slf4j.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ScopeAsyncAppender extends AsyncAppender implements LoggingScopeSink {

    private List<LoggingScopeProvider> scopeProviders = new ArrayList<>();

    public void addScopeProvider(LoggingScopeProvider scopeProvider) {
        this.scopeProviders.add(scopeProvider);
    }

    @Override
    public void preprocess(ILoggingEvent eventObject) {
        // try to pick up MDC fields

        Map<String, String> mdcContext = null;
        for (LoggingScopeProvider loggingScopeFactory : scopeProviders) {
            LoggingScope scope = loggingScopeFactory.getCurrentScope();
            if(scope != null && scope instanceof MdcLoggingScope mdcLoggingScope) {
                Map<String, String> scopeMdcContext = mdcLoggingScope.getMdcContext();
                if(scopeMdcContext != null && !scopeMdcContext.isEmpty()) {
                    if(mdcContext == null) {
                        mdcContext = scopeMdcContext;
                    } else {
                        mdcContext = new HashMap<>(mdcContext);
                        mdcContext.putAll(scopeMdcContext);
                    }
                }
            }
        }

        if (mdcContext == null) {
            super.preprocess(eventObject);
        } else {
            for (Map.Entry<String, String> entry : mdcContext.entrySet()) {
                MDC.put(entry.getKey(), entry.getValue());
            }
            // TODO this currently does not work with testing, since it is not possible
            // to set the MDC map twice in the original ILoggingEvent
            try {
                super.preprocess(eventObject);
            } finally {
                for (Map.Entry<String, String> entry : mdcContext.entrySet()) {
                    MDC.remove(entry.getKey());
                }
            }
        }
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
