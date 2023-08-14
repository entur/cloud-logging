package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.AsyncAppender;

import java.util.concurrent.ConcurrentLinkedQueue;

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
            put(eventObject);
        }
    }

    public void flushScope() {
        LoggingScope scope = loggingScopeFactory.getScope();

        if (scope != null) {
            ConcurrentLinkedQueue<ILoggingEvent> events = scope.getEvents();
            for (ILoggingEvent event : events) {
                put(event);
            }
        }
    }

    public void closeScope() {
        loggingScopeFactory.closeScope();
    }

    public <T> T openScope() {
        return (T) loggingScopeFactory.openScope();
    }

}
