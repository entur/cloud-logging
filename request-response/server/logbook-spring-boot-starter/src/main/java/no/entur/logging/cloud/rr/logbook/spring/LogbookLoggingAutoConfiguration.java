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

    @Value("entur.logging.request-response.logger.level")
    protected String loggerLevel;

    @Value("entur.logging.request-response.logger.name")
    protected String loggerName;


    // ignore HttpLogFormatter and HttpLogWriter
    @Bean
    @ConditionalOnMissingBean(Sink.class)
    public Sink sink() {
        Logger logger = LoggerFactory.getLogger(loggerName);
        Level level = parseLevel(loggerLevel);

        return LogLevelLogstashLogbackSink.newBuilder()
                .withLogger(logger)
                .withLogLevel(level)
                .withValidateRequestJsonBody(true)
                .withValidateResponseJsonBody(false)
                .build();
    }

    public static Level parseLevel(String loggerLevel) {
        switch (loggerLevel) {
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
