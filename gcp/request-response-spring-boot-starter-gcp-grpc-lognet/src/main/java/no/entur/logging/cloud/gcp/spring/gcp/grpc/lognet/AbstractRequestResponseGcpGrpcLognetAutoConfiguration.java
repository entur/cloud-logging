package no.entur.logging.cloud.gcp.spring.gcp.grpc.lognet;

import com.google.protobuf.util.JsonFormat;
import no.entur.logging.cloud.rr.grpc.GrpcLoggingServerInterceptor;
import no.entur.logging.cloud.rr.grpc.GrpcSink;
import no.entur.logging.cloud.rr.grpc.LogbackLogstashGrpcSink;
import no.entur.logging.cloud.rr.grpc.filter.GrpcServerLoggingFilters;
import no.entur.logging.cloud.rr.grpc.mapper.DefaultGrpcPayloadJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.DefaultMetadataJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcMetadataJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcPayloadJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcStatusMapper;
import no.entur.logging.cloud.rr.grpc.mapper.JsonPrinterFactory;
import no.entur.logging.cloud.rr.grpc.mapper.JsonPrinterStatusMapper;
import no.entur.logging.cloud.rr.grpc.mapper.TypeRegistryFactory;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.HashMap;


public abstract class AbstractRequestResponseGcpGrpcLognetAutoConfiguration {

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
