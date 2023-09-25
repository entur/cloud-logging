package no.entur.logging.cloud.logbook;

import com.fasterxml.jackson.core.JsonFactory;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import static org.slf4j.event.EventConstants.*;

public abstract class AbstractSinkBuilder<B, E extends AbstractSinkBuilder<B, E>> {

    protected Logger logger;

    protected Level level;

    protected JsonFactory jsonFactory;

    protected int maxSize = -1;
    protected int maxBodySize = -1;

    protected RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier;

    public B withLogger(Logger logger) {
        this.logger = logger;
        return (B) this;
    }

    public B withJsonFactory(JsonFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
        return (B) this;
    }

    public B withRemoteHttpMessageContextSupplier(RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier) {
        this.remoteHttpMessageContextSupplier = remoteHttpMessageContextSupplier;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B withLogLevel(Level level) {
        this.level = level;
        return (B) this;
    }

    public B withMaxBodySize(int size) {
        this.maxBodySize = size;
        return (B) this;
    }

    public B withMaxSize(int size) {
        this.maxSize = size;
        return (B) this;
    }

    protected BooleanSupplier logEnabledToBooleanSupplier() {
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

    protected BiConsumer<Marker, String> loggerToBiConsumer() {

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
