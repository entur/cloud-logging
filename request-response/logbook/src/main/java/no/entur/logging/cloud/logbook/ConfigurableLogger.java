package no.entur.logging.cloud.logbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import static org.slf4j.event.EventConstants.DEBUG_INT;
import static org.slf4j.event.EventConstants.ERROR_INT;
import static org.slf4j.event.EventConstants.INFO_INT;
import static org.slf4j.event.EventConstants.TRACE_INT;
import static org.slf4j.event.EventConstants.WARN_INT;

public class ConfigurableLogger {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private Logger logger;

        private Level level;

        public Builder withLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder withLogLevel(Level level) {
            this.level = level;
            return this;
        }

        public ConfigurableLogger build() {
            if(logger == null) {
                logger = LoggerFactory.getLogger(ConfigurableLogger.class);
            }
            if(level == null) {
                level = Level.INFO;
            }
            return new ConfigurableLogger(loggerToBiConsumer(), logEnabledToBooleanSupplier());
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


    public ConfigurableLogger(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled) {
        this.logConsumer = logConsumer;
        this.logLevelEnabled = logLevelEnabled;
    }

    public BiConsumer<Marker, String> getLogConsumer() {
        return logConsumer;
    }

    public BooleanSupplier getLogLevelEnabled() {
        return logLevelEnabled;
    }
}
