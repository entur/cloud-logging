package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 *
 * For event which are actually written, i.e. including on-demand logging.
 *
 */

public interface CompositeConsoleLoggingEventListener {

    void put(ILoggingEvent event);
}
