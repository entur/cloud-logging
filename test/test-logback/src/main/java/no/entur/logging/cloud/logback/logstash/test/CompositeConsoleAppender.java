package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.core.encoder.Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeConsoleAppender<E> extends ch.qos.logback.core.ConsoleAppender<E> {

    private static CompositeConsoleAppender INSTANCE;

    public static CompositeConsoleAppender getInstance() {
        // TODO crude mechanism, could this be pulled from the logging context?
       return INSTANCE;
    }

    public CompositeConsoleAppender() {
        if(INSTANCE != null) {
            throw new IllegalStateException();
        }
        INSTANCE = this;
    }

    protected Encoder<E> humanReadablePlainEncoder;

    protected Encoder<E> humanReadableJsonEncoder;

    protected Encoder<E> machineReadableJsonEncoder;

    public Encoder<E> getHumanReadablePlainEncoder() {
        return humanReadablePlainEncoder;
    }

    public void useHumanReadablePlainEncoder() {
        setEncoder(humanReadablePlainEncoder);
    }

    public void useHumanReadableJsonEncoder() {
        setEncoder(humanReadableJsonEncoder);
    }

    public void useMachineReadableJsonEncoder() {
        setEncoder(machineReadableJsonEncoder);
    }

    public void setHumanReadablePlainEncoder(Encoder<E> humanReadablePlainEncoder) {
        this.humanReadablePlainEncoder = humanReadablePlainEncoder;
        if(this.encoder == null) {
            this.setEncoder(humanReadablePlainEncoder);
        }
    }

    public Encoder<E> getHumanReadableJsonEncoder() {
        return humanReadableJsonEncoder;
    }

    public void setHumanReadableJsonEncoder(Encoder<E> humanReadableJsonEncoder) {
        this.humanReadableJsonEncoder = humanReadableJsonEncoder;
        if(this.encoder == null) {
            this.setEncoder(humanReadableJsonEncoder);
        }
    }

    public Encoder<E> getMachineReadableJsonEncoder() {
        return machineReadableJsonEncoder;
    }

    public void setMachineReadableJsonEncoder(Encoder<E> machineReadableJsonEncoder) {
        this.machineReadableJsonEncoder = machineReadableJsonEncoder;
        if(this.encoder == null) {
            this.setEncoder(machineReadableJsonEncoder);
        }
    }

    @Override
    public void doAppend(E eventObject) {
        super.doAppend(eventObject);
    }

    @Override
    public void setEncoder(Encoder<E> encoder) {
        super.setEncoder(encoder);
    }
}