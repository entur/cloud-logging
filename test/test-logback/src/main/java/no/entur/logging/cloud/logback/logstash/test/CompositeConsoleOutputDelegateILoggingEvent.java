package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface CompositeConsoleOutputDelegateILoggingEvent extends ILoggingEvent {

    CompositeConsoleOutputType getCompositeConsoleOutputType();
}
