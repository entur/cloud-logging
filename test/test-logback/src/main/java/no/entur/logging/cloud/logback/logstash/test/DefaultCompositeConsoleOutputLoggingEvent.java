package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class DefaultCompositeConsoleOutputLoggingEvent implements CompositeConsoleOutputDelegateILoggingEvent  {

    private final ILoggingEvent event;
    private final CompositeConsoleOutputType compositeConsoleOutputType;
    public DefaultCompositeConsoleOutputLoggingEvent(ILoggingEvent event, CompositeConsoleOutputType compositeConsoleOutputType) {
        this.event = event;
        this.compositeConsoleOutputType = compositeConsoleOutputType;
    }
    @Override
    public String getThreadName() {
        return event.getThreadName();
    }

    @Override
    public Level getLevel() {
        return event.getLevel();
    }

    @Override
    public String getMessage() {
        return event.getMessage();
    }

    @Override
    public Object[] getArgumentArray() {
        return event.getArgumentArray();
    }

    @Override
    public String getFormattedMessage() {
        return event.getFormattedMessage();
    }

    @Override
    public String getLoggerName() {
        return event.getLoggerName();
    }

    @Override
    public LoggerContextVO getLoggerContextVO() {
        return event.getLoggerContextVO();
    }

    @Override
    public IThrowableProxy getThrowableProxy() {
        return event.getThrowableProxy();
    }

    @Override
    public StackTraceElement[] getCallerData() {
        return event.getCallerData();
    }

    @Override
    public boolean hasCallerData() {
        return event.hasCallerData();
    }

    @Override
    public Marker getMarker() {
        return event.getMarker();
    }

    @Override
    public List<Marker> getMarkerList() {
        return event.getMarkerList();
    }

    @Override
    public Map<String, String> getMDCPropertyMap() {
        return event.getMDCPropertyMap();
    }

    @Override
    public Map<String, String> getMdc() {
        return event.getMdc();
    }

    @Override
    public long getTimeStamp() {
        return event.getTimeStamp();
    }

    @Override
    public int getNanoseconds() {
        return event.getNanoseconds();
    }

    @Override
    public Instant getInstant() {
        return event.getInstant();
    }

    @Override
    public long getSequenceNumber() {
        return event.getSequenceNumber();
    }

    @Override
    public List<KeyValuePair> getKeyValuePairs() {
        return event.getKeyValuePairs();
    }

    @Override
    public void prepareForDeferredProcessing() {
        event.prepareForDeferredProcessing();
    }


    @Override
    public CompositeConsoleOutputType getCompositeConsoleOutputType() {
        return compositeConsoleOutputType;
    }
}
