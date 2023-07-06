package no.entur.logging.cloud.rr.logbook.spring;

import no.entur.logging.cloud.logbook.LogLevelLogstashLogbackSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.Sink;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;

@AutoConfigureBefore(value = {
        LogbookAutoConfiguration.class
})

@Configuration
public class LogbookLoggingAutoConfiguration {

    @Value("${entur.logging.request-response.logger.level}")
    protected String loggerLevel;

    @Value("${entur.logging.request-response.logger.name}")
    protected String loggerName;

    @Value("${entur.logging.request-response.max-size}")
    protected int maxSize;

    @Value("${entur.logging.request-response.max-body-size}")
    protected int maxBodySize;


    // ignore HttpLogFormatter and HttpLogWriter
    @Bean
    @ConditionalOnMissingBean(Sink.class)
    public Sink sink() {
        Logger logger = LoggerFactory.getLogger(loggerName);
        Level level = parseLevel(loggerLevel);

        // TODO externalize decision on whether to trust incoming JSON is well-formed
        // for example an authorized client would be trusted

        return LogLevelLogstashLogbackSink.newBuilder()
                .withLogger(logger)
                .withLogLevel(level)
                .withMaxBodySize(maxBodySize)
                .withMaxSize(maxSize)
                .withValidateRequestJsonBody(true)
                .withValidateResponseJsonBody(false)
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
