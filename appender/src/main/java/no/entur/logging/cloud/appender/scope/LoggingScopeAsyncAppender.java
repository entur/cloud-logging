package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import no.entur.logging.cloud.appender.MdcAsyncAppender;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

public class LoggingScopeAsyncAppender extends MdcAsyncAppender implements LoggingScopeSink {

    public static LoggingScopeAsyncAppender get() {
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders();
        if(!appenderIterator.hasNext()) {
            throw new IllegalStateException("No on-demand log appenders configured, expected at least one which is implementing " + LoggingScopeAsyncAppender.class.getName());
        }
        while (appenderIterator.hasNext()) {
            Appender<ILoggingEvent> appender = appenderIterator.next();
            if (appender instanceof LoggingScopeAsyncAppender) {
                return (LoggingScopeAsyncAppender) appender;
            }
        }
        throw new IllegalStateException("Expected on-demand log appender implementing " + LoggingScopeAsyncAppender.class.getName());
    }

    private List<LoggingScopeProvider> scopeProviders = new ArrayList<>();
    private LoggingScopeMetrics metrics;

    public void setMetrics(LoggingScopeMetrics metrics) {
        this.metrics = metrics;
    }

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
            write(eventObject);
        }
    }

    public void write(ILoggingEvent eventObject) {
        postProcess(eventObject);
        put(eventObject);
        if(metrics != null) {
            metrics.increment(eventObject);
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
        Queue<ILoggingEvent> events = scope.getEvents();
        for (ILoggingEvent eventObject : events) {
            write(eventObject);
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
