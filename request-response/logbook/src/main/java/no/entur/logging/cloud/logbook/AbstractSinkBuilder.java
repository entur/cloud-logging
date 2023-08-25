package no.entur.logging.cloud.logbook;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import static org.slf4j.event.EventConstants.DEBUG_INT;
import static org.slf4j.event.EventConstants.ERROR_INT;
import static org.slf4j.event.EventConstants.INFO_INT;
import static org.slf4j.event.EventConstants.TRACE_INT;
import static org.slf4j.event.EventConstants.WARN_INT;

public abstract class AbstractSinkBuilder<B, E extends AbstractSinkBuilder<B, E>> {

    protected Logger logger;

    protected Level level;

    protected BooleanSupplier requestBodyWellformedDecisionSupplier;
    protected BooleanSupplier responseBodyWellformedDecisionSupplier;

    protected int maxSize = -1;
    protected int maxBodySize = -1;

    public B withLogger(Logger logger) {
        this.logger = logger;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B withValidateRequestJsonBodyWellformed(BooleanSupplier validateRequestJsonBodyWellformed) {
        this.requestBodyWellformedDecisionSupplier = validateRequestJsonBodyWellformed;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B withValidateResponseJsonBodyWellformed(BooleanSupplier validateResponseJsonBodyWellformed) {
        this.responseBodyWellformedDecisionSupplier = validateResponseJsonBodyWellformed;
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
