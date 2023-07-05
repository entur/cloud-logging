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

public class CompositeSink implements Sink {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private Logger logger;

        private Level level;

        private boolean validateRequestJsonBody;
        private boolean validateResponseJsonBody;

        private Sink humanReadablePlainSink;
        private Sink humanReadableJsonSink;

        private Sink machineReadableJsonSink;

        public Builder withLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

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

        public Builder withValidateRequestJsonBody(boolean validateRequestJsonBody) {
            this.validateRequestJsonBody = validateRequestJsonBody;
            return this;
        }

        public Builder withValidateResponseJsonBody(boolean validateResponseJsonBody) {
            this.validateResponseJsonBody = validateResponseJsonBody;
            return this;
        }

        public Builder withLogLevel(Level level) {
            this.level = level;
            return this;
        }

        public CompositeSink build() {
            if(logger == null) {
                logger = LoggerFactory.getLogger("no.entur.logging.cloud.logbook");
            }
            if(level == null) {
                level = Level.INFO;
            }

            if(humanReadablePlainSink == null) {
                throw new IllegalStateException();
            }
            if(humanReadableJsonSink == null) {
                throw new IllegalStateException();
            }
            if(machineReadableJsonSink == null) {
                throw new IllegalStateException();
            }

            return new CompositeSink(loggerToBiConsumer(), logEnabledToBooleanSupplier(), validateRequestJsonBody, validateResponseJsonBody, humanReadablePlainSink, humanReadableJsonSink, machineReadableJsonSink);
        }

        private BooleanSupplier logEnabledToBooleanSupplier() {
            int levelInt = level.toInt();
            switch (levelInt) {
                case (TRACE_INT):
                    return logger::isTraceEnabled;
                case (DEBUG_INT):
                    return logger::isDebugEnabled;
                case (INFO_INT):
                    return logger::isInfoEnabled;
                case (WARN_INT):
                    return logger::isWarnEnabled;
                case (ERROR_INT):
                    return logger::isErrorEnabled;
                default:
                    throw new IllegalStateException("Level [" + level + "] not recognized.");
            }
        }

        private BiConsumer<Marker, String> loggerToBiConsumer() {

            int levelInt = level.toInt();
            switch (levelInt) {
                case (TRACE_INT):
                    return logger::trace;
                case (DEBUG_INT):
                    return  logger::debug;
                case (INFO_INT):
                    return logger::info;
                case (WARN_INT):
                    return  logger::warn;
                case (ERROR_INT):
                    return logger::error;
                default:
                    throw new IllegalStateException("Level [" + level + "] not recognized.");
            }

        }
    }

    protected final BiConsumer<Marker, String> logConsumer;
    protected final BooleanSupplier logLevelEnabled;

    protected final boolean validateRequestJsonBody;
    protected final boolean validateResponseJsonBody;

    private Sink humanReadablePlainSink;
    private Sink humanReadableJsonSink;

    private Sink machineReadableJsonSink;

    public CompositeSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled, boolean validateRequestJsonBody, boolean validateResponseJsonBody, Sink humanReadablePlainSink, Sink humanReadableJsonSink, Sink machineReadableJsonSink) {
        this.logConsumer = logConsumer;
        this.logLevelEnabled = logLevelEnabled;
        this.validateRequestJsonBody = validateRequestJsonBody;
        this.validateResponseJsonBody = validateResponseJsonBody;

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
