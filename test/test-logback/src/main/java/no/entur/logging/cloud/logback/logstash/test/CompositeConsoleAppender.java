package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import org.slf4j.Marker;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class CompositeConsoleAppender<E extends ILoggingEvent> extends ch.qos.logback.core.ConsoleAppender<E> {

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

            CompositeConsoleOutputControl.useHumanReadablePlainEncoder();
        }
    }

    public Encoder<E> getHumanReadableJsonEncoder() {
        return humanReadableJsonEncoder;
    }

    public void setHumanReadableJsonEncoder(Encoder<E> humanReadableJsonEncoder) {
        this.humanReadableJsonEncoder = humanReadableJsonEncoder;
        if(this.encoder == null) {
            this.setEncoder(humanReadableJsonEncoder);

            CompositeConsoleOutputControl.useHumanReadableJsonEncoder();
        }
    }

    public Encoder<E> getMachineReadableJsonEncoder() {
        return machineReadableJsonEncoder;
    }

    public void setMachineReadableJsonEncoder(Encoder<E> machineReadableJsonEncoder) {
        this.machineReadableJsonEncoder = machineReadableJsonEncoder;
        if(this.encoder == null) {
            this.setEncoder(machineReadableJsonEncoder);

            CompositeConsoleOutputControl.useMachineReadableJsonEncoder();
        }
    }

    protected void writeOut(E event) throws IOException {
        // TODO should messages be flushed before the output type changes?

        CompositeConsoleOutputType output = getOutputType(event);
        switch (output) {
            case humanReadablePlain: {
                writeBytes(this.humanReadablePlainEncoder.encode(event));
                break;
            }
            case humanReadableJson: {
                writeBytes(this.humanReadableJsonEncoder.encode(event));
                break;
            }
            case machineReadableJson: {
                writeBytes(this.machineReadableJsonEncoder.encode(event));
                break;
            }
            default: {
                writeBytes(this.encoder.encode(event));
                break;
            }
        }

    }

    private CompositeConsoleOutputType getOutputType(E event) {
        List<Marker> markerList = event.getMarkerList();
        if(markerList != null) {
            for (Marker marker : markerList) {
                if (marker instanceof CompositeConsoleOutputMarker) {
                    CompositeConsoleOutputMarker m = (CompositeConsoleOutputMarker) marker;
                    return m.getCompositeConsoleOutputType();
                }
            }
        }

        return CompositeConsoleOutputControl.getOutput();
    }

    // copy of superclass method
    private void writeBytes(byte[] byteArray) throws IOException {
        if (byteArray == null || byteArray.length == 0)
            return;

        lock.lock();
        try {
            OutputStream outputStream = getOutputStream();
            outputStream.write(byteArray);
            if (isImmediateFlush()) {
                outputStream.flush();
            }
        } finally {
            lock.unlock();
        }
    }

}