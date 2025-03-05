package no.entur.logging.cloud.appender.scope;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 *
 * Interface for updating metrics for log statements which are actually written.
 *
 */

public interface LoggingScopeMetrics {

    void increment(ILoggingEvent iLoggingEvent);
}
