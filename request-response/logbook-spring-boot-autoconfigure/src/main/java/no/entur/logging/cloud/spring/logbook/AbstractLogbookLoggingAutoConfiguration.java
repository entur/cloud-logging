package no.entur.logging.cloud.spring.logbook;

import no.entur.logging.cloud.logbook.RemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.ondemand.OndemandLogLevelLogstashLogbackSink;
import no.entur.logging.cloud.logbook.LogLevelLogstashLogbackSink;
import no.entur.logging.cloud.logbook.ondemand.state.RequestHttpMessageStateSupplierSource;
import no.entur.logging.cloud.logbook.ondemand.state.ResponseHttpMessageStateSupplierSource;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;

public class AbstractLogbookLoggingAutoConfiguration {

    @Value("${entur.logging.request-response.logger.level:info}")
    protected String loggerLevel;

    @Value("${entur.logging.request-response.logger.name:no.entur.logging.cloud}")
    protected String loggerName;

    @Value("${entur.logging.request-response.max-size}")
    protected int maxSize;

    @Value("${entur.logging.request-response.max-body-size}")
    protected int maxBodySize;

    // TODO parameter for sync vs async validation of JSON body wellformedness

    protected LogLevelLogstashLogbackSink createMachineReadbleSink(Logger logger, Level level, RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier) {
        return LogLevelLogstashLogbackSink.newBuilder()
                .withLogger(logger)
                .withLogLevel(level)
                .withMaxBodySize(maxBodySize)
                .withMaxSize(maxSize)
                .withRemoteHttpMessageContextSupplier(remoteHttpMessageContextSupplier)
                .build();
    }

    protected OndemandLogLevelLogstashLogbackSink createAsyncMachineReadbleSink(Logger logger, Level level, RequestHttpMessageStateSupplierSource validateRequestJsonBodyWellformed, ResponseHttpMessageStateSupplierSource validateResponseJsonBodyWellformed, RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier) {
        return OndemandLogLevelLogstashLogbackSink.newBuilder()
                .withLogger(logger)
                .withLogLevel(level)
                .withMaxBodySize(maxBodySize)
                .withMaxSize(maxSize)
                .withValidateRequestJsonBodyWellformed(validateRequestJsonBodyWellformed)
                .withValidateResponseJsonBodyWellformed(validateResponseJsonBodyWellformed)
                .withRemoteHttpMessageContextSupplier(remoteHttpMessageContextSupplier)
                .build();
    }

    public static Level parseLevel(String loggerLevel) {
        switch (loggerLevel.toLowerCase()) {
            case "trace": return Level.TRACE;
            case "debug": return Level.DEBUG;
            case "info": return Level.INFO;
            case "warn": return Level.WARN;
            case "error": return Level.ERROR;
            default : {
                throw new IllegalStateException("Unknown logger level " + loggerLevel);
            }
        }
    }
}
