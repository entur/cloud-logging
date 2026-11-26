package no.entur.logging.cloud.logback.logstash.test.ai;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Forwards each event to all attached appenders. Needed because logback's async appenders accept only a single child.
 */
public class FanOutAppender extends ForwardingAppender {

    @Override
    protected void append(ILoggingEvent event) {
        appenders.appendLoopOnAppenders(event);
    }
}
