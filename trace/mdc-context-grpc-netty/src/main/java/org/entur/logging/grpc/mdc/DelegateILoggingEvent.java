package org.entur.logging.grpc.mdc;

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

public class DelegateILoggingEvent implements ILoggingEvent {

    private final ILoggingEvent event;
    private final Map<String, String> grpcMdcContext;

    public DelegateILoggingEvent(ILoggingEvent event, Map<String, String> grpcMdcContext) {
        this.event = event;
        this.grpcMdcContext = grpcMdcContext;
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
        Map<String, String> mdcPropertyMap = event.getMDCPropertyMap();

        HashMap<String, String> result = new HashMap<>((mdcPropertyMap.size() + grpcMdcContext.size()) * 2);

        result.putAll(mdcPropertyMap);
        result.putAll(grpcMdcContext);

        return result;
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
}
