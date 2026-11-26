package no.entur.logging.cloud.logback.logstash.test.ai;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;

import java.util.Iterator;

/**
 * An appender which passes events on to the attached appenders (see {@link FanOutAppender} and {@code DeduplicatingAppender}).
 * Stopping it stops the attached appenders.
 */
public abstract class ForwardingAppender extends AppenderBase<ILoggingEvent> implements AppenderAttachable<ILoggingEvent> {

    protected final AppenderAttachableImpl<ILoggingEvent> appenders = new AppenderAttachableImpl<>();

    @Override
    public void stop() {
        appenders.detachAndStopAllAppenders();
        super.stop();
    }

    @Override
    public void addAppender(Appender<ILoggingEvent> appender) {
        appenders.addAppender(appender);
    }

    @Override
    public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
        return appenders.iteratorForAppenders();
    }

    @Override
    public Appender<ILoggingEvent> getAppender(String name) {
        return appenders.getAppender(name);
    }

    @Override
    public boolean isAttached(Appender<ILoggingEvent> appender) {
        return appenders.isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        appenders.detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(Appender<ILoggingEvent> appender) {
        return appenders.detachAppender(appender);
    }

    @Override
    public boolean detachAppender(String name) {
        return appenders.detachAppender(name);
    }
}
