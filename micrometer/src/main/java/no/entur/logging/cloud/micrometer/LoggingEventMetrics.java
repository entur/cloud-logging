package no.entur.logging.cloud.micrometer;

import ch.qos.logback.classic.spi.ILoggingEvent;

public interface LoggingEventMetrics {

    void increment(ILoggingEvent event);
    
    
}
