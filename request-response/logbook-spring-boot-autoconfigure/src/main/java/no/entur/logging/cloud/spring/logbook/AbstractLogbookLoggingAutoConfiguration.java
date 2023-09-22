package no.entur.logging.cloud.spring.logbook;

import no.entur.logging.cloud.logbook.async.AsyncLogLevelLogstashLogbackSink;
import no.entur.logging.cloud.logbook.LogLevelLogstashLogbackSink;
import no.entur.logging.cloud.logbook.async.state.RequestHttpMessageStateSupplierSource;
import no.entur.logging.cloud.logbook.async.state.ResponseHttpMessageStateSupplierSource;
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

    protected LogLevelLogstashLogbackSink createMachineReadbleSink(Logger logger, Level level) {
        return LogLevelLogstashLogbackSink.newBuilder()
                .withLogger(logger)
                .withLogLevel(level)
                .withMaxBodySize(maxBodySize)
                .withMaxSize(maxSize)
                .build();
    }

    protected AsyncLogLevelLogstashLogbackSink createAsyncMachineReadbleSink(Logger logger, Level level, RequestHttpMessageStateSupplierSource validateRequestJsonBodyWellformed, ResponseHttpMessageStateSupplierSource validateResponseJsonBodyWellformed) {
        return AsyncLogLevelLogstashLogbackSink.newBuilder()
                .withLogger(logger)
                .withLogLevel(level)
                .withMaxBodySize(maxBodySize)
                .withMaxSize(maxSize)
                .withValidateRequestJsonBodyWellformed(validateRequestJsonBodyWellformed)
                .withValidateResponseJsonBodyWellformed(validateResponseJsonBodyWellformed)
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
