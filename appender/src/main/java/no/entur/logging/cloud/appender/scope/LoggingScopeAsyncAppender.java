package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.AsyncAppender;
import org.slf4j.Marker;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

public class LoggingScopeAsyncAppender extends AsyncAppender {

    protected LoggingScopeFactory loggingScopeFactory = new NoopLoggingScopeFactory();

    public void setLoggingScopeFactory(LoggingScopeFactory loggingScopeFactory) {
        this.loggingScopeFactory = loggingScopeFactory;
    }

    public LoggingScopeAsyncAppender() {
        // do nothing
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (isQueueBelowDiscardingThreshold() && isDiscardable(eventObject)) {
            return;
        }
        preprocess(eventObject);

        LoggingScope scope = loggingScopeFactory.getScope();
        if(scope == null || !scope.append(eventObject)) {
            postProcess(eventObject);
            put(eventObject);
        }
    }

    public void flushScope() {
        LoggingScope scope = loggingScopeFactory.getScope();
        if (scope != null) {
            ConcurrentLinkedQueue<ILoggingEvent> events = scope.getEvents();
            for (ILoggingEvent eventObject : events) {
                postProcess(eventObject);
                put(eventObject);
            }
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

    public void closeScope() {
        loggingScopeFactory.closeScope();
    }

    public <T> LoggingScopeFactory<T> getLoggingScopeFactory() {
        return loggingScopeFactory;
    }
}
