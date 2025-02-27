package no.entur.logging.cloud.spring.ondemand.web.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.appender.scope.LoggingScope;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NoopLoggingScope implements LoggingScope {

    protected static final ConcurrentLinkedQueue<ILoggingEvent> EMPTY = new ConcurrentLinkedQueue<ILoggingEvent>();

    @Override
    public Queue<ILoggingEvent> getEvents() {
        return EMPTY;
    }

    @Override
    public boolean append(ILoggingEvent eventObject) {
        return false;
    }

    @Override
    public boolean isFailure() {
        return false;
    }

    @Override
    public void failure() {
        // do nothing
    }

    @Override
    public long getTimestamp() {
        return -1L;
    }

    @Override
    public void write() {
        // do nothing
    }
}
