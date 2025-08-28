package no.entur.logging.cloud.spring.logbook;

import no.entur.logging.cloud.logbook.RemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.ondemand.OndemandLogLevelLogstashLogbackSink;
import no.entur.logging.cloud.logbook.LogLevelLogstashLogbackSink;
import no.entur.logging.cloud.logbook.ondemand.state.RequestHttpMessageStateSupplierSource;
import no.entur.logging.cloud.logbook.ondemand.state.ResponseHttpMessageStateSupplierSource;
import no.entur.logging.cloud.spring.logbook.properties.FormatProperties;

import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class AbstractLogbookLoggingAutoConfiguration {

    @Value("${entur.logging.request-response.logger.level:info}")
    protected String loggerLevel;

    @Value("${entur.logging.request-response.logger.name:no.entur.logging.cloud}")
    protected String loggerName;

    @Value("${entur.logging.request-response.max-size:-1}")
    private int maxSize;

    @Value("${entur.logging.request-response.max-body-size:-1}")
    private int maxBodySize;

    // TODO parameter for sync vs async validation of JSON body wellformedness
    @Autowired
    protected LogbookLoggingCloudProperties logbookLoggingCloudProperties;

    @Autowired
    protected FormatProperties format;

    protected int getMaxBodySize() {
        if (maxBodySize == -1) {
            return logbookLoggingCloudProperties.getMaxBodySize();
        }
        return Math.min(logbookLoggingCloudProperties.getMaxBodySize(), maxBodySize);
    }

    protected int getMaxSize() {
        if (maxSize == -1) {
            return logbookLoggingCloudProperties.getMaxSize();
        }
        return Math.min(logbookLoggingCloudProperties.getMaxSize(), maxSize);
    }

    protected LogLevelLogstashLogbackSink createMachineReadbleSink(Logger logger, Level level,
            RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier) {
        return LogLevelLogstashLogbackSink.newBuilder()
                .withLogger(logger)
                .withLogLevel(level)
                .withMaxBodySize(getMaxBodySize())
                .withMaxSize(getMaxSize())
                .withRemoteHttpMessageContextSupplier(remoteHttpMessageContextSupplier)
                .withMessageComposers(format.getServer().getMessage().toComposer(), format.getClient().getMessage().toComposer())
                .build();
    }

    protected OndemandLogLevelLogstashLogbackSink createAsyncMachineReadableSink(Logger logger, Level level,
            RequestHttpMessageStateSupplierSource validateRequestJsonBodyWellformed,
            ResponseHttpMessageStateSupplierSource validateResponseJsonBodyWellformed,
            RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier) {
        return OndemandLogLevelLogstashLogbackSink.newBuilder()
                .withLogger(logger)
                .withLogLevel(level)
                .withMaxBodySize(getMaxBodySize())
                .withMaxSize(getMaxSize())
                .withValidateRequestJsonBodyWellformed(validateRequestJsonBodyWellformed)
                .withValidateResponseJsonBodyWellformed(validateResponseJsonBodyWellformed)
                .withRemoteHttpMessageContextSupplier(remoteHttpMessageContextSupplier)
                .withMessageComposers(format.getServer().getMessage().toComposer(), format.getClient().getMessage().toComposer())
                .build();
    }

    public static Level parseLevel(String loggerLevel) {
        switch (loggerLevel.toLowerCase()) {
            case "trace":
                return Level.TRACE;
            case "debug":
                return Level.DEBUG;
            case "info":
                return Level.INFO;
            case "warn":
                return Level.WARN;
            case "error":
                return Level.ERROR;
            default: {
                throw new IllegalStateException("Unknown logger level " + loggerLevel);
            }
        }
    }
}
