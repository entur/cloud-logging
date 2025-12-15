package no.entur.logging.cloud.logbook.logbook.test;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Precorrelation;
import org.zalando.logbook.Sink;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import static org.slf4j.event.EventConstants.DEBUG_INT;
import static org.slf4j.event.EventConstants.ERROR_INT;
import static org.slf4j.event.EventConstants.INFO_INT;
import static org.slf4j.event.EventConstants.TRACE_INT;
import static org.slf4j.event.EventConstants.WARN_INT;

/** Sink which effectively adjusts the formatting of the request-response to the current encoder scheme */

public class CompositeSink implements Sink {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private Sink humanReadablePlainSink;
        private Sink humanReadableJsonSink;

        private Sink machineReadableJsonSink;

        public Builder withHumanReadableJsonSink(Sink humanReadableJsonSink) {
            this.humanReadableJsonSink = humanReadableJsonSink;
            return this;
        }

        public Builder withHumanReadablePlainSink(Sink humanReadablePlainSink) {
            this.humanReadablePlainSink = humanReadablePlainSink;
            return this;
        }

        public Builder withMachineReadableJsonSink(Sink machineReadableJsonSink) {
            this.machineReadableJsonSink = machineReadableJsonSink;
            return this;
        }

        public CompositeSink build() {
            if(humanReadablePlainSink == null) {
                throw new IllegalStateException();
            }
            if(humanReadableJsonSink == null) {
                throw new IllegalStateException();
            }
            if(machineReadableJsonSink == null) {
                throw new IllegalStateException();
            }

            return new CompositeSink(humanReadablePlainSink, humanReadableJsonSink, machineReadableJsonSink);
        }
    }

    private Sink humanReadablePlainSink;
    private Sink humanReadableJsonSink;

    private Sink machineReadableJsonSink;

    public CompositeSink(Sink humanReadablePlainSink, Sink humanReadableJsonSink, Sink machineReadableJsonSink) {
        this.humanReadablePlainSink = humanReadablePlainSink;
        this.humanReadableJsonSink = humanReadableJsonSink;
        this.machineReadableJsonSink = machineReadableJsonSink;
    }

    @Override
    public boolean isActive() {
        return machineReadableJsonSink.isActive();
    }

    @Override
    public void write(Precorrelation precorrelation, HttpRequest request) throws IOException {
        CompositeConsoleOutputType output = CompositeConsoleOutputControl.getOutput();
        switch (output) {
            case humanReadablePlain: {
                writeHumanReadablePlainEncoder(precorrelation, request);
                break;
            }
            case humanReadableJson: {
                writeHumanReadableJsonEncoder(precorrelation, request);
                break;
            }
            case machineReadableJson: {
                writeMachineReadableJsonEncoder(precorrelation, request);
                break;
            }
        }
    }

    protected void writeHumanReadablePlainEncoder(Precorrelation precorrelation, HttpRequest request) throws IOException {
        humanReadablePlainSink.write(precorrelation, request);
    }

    protected void writeHumanReadableJsonEncoder(Precorrelation precorrelation, HttpRequest request) throws IOException {
        humanReadableJsonSink.write(precorrelation, request);
    }

    protected void writeMachineReadableJsonEncoder(Precorrelation precorrelation, HttpRequest request) throws IOException {
        machineReadableJsonSink.write(precorrelation, request);
    }
    @Override
    public void write(Correlation correlation, HttpRequest request, HttpResponse response) throws IOException {
        CompositeConsoleOutputType output = CompositeConsoleOutputControl.getOutput();
        switch (output) {
            case humanReadablePlain: {
                writeHumanReadablePlainEncoder(correlation, request, response);
                break;
            }
            case humanReadableJson: {
                writeHumanReadableJsonEncoder(correlation, request, response);
                break;
            }
            case machineReadableJson: {
                writeMachineReadableJsonEncoder(correlation, request, response);
                break;
            }
        }

    }

    protected void writeHumanReadablePlainEncoder(Correlation precorrelation, HttpRequest request, HttpResponse response) throws IOException {
        humanReadablePlainSink.write(precorrelation, request, response);
    }

    protected void writeHumanReadableJsonEncoder(Correlation precorrelation, HttpRequest request, HttpResponse response) throws IOException {
        humanReadableJsonSink.write(precorrelation, request, response);
    }

    protected void writeMachineReadableJsonEncoder(Correlation precorrelation, HttpRequest request, HttpResponse response) throws IOException {
        machineReadableJsonSink.write(precorrelation, request, response);
    }

}
