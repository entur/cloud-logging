package no.entur.logging.cloud.spring.rr.grpc;

import no.entur.logging.cloud.rr.grpc.LogbackLogstashGrpcSink;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;

public abstract class AbstractRequestResponseGrpcSinkAutoConfiguration {

    @Value("${entur.logging.request-response.logger.level:info}")
    protected String loggerLevel;

    @Value("${entur.logging.request-response.logger.name:no.entur.logging.cloud}")
    protected String loggerName;

    protected LogbackLogstashGrpcSink createMachineReadbleSink(Logger logger, Level level) {
        return LogbackLogstashGrpcSink.newBuilder()
                .withLogger(logger)
                .withLogLevel(level)
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
