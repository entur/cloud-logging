package no.entur.logging.cloud.rr.logbook.spring.test;


import no.entur.logging.cloud.logbook.LogLevelLogstashLogbackSink;
import no.entur.logging.cloud.logbook.logbook.test.CompositeSink;
import no.entur.logging.cloud.logbook.logbook.test.ConsoleOutputTypeLogLevelLogstashLogbackSink;
import no.entur.logging.cloud.logbook.logbook.test.PrettyPrintingLogLevelLogstashLogbackSink;
import no.entur.logging.cloud.logbook.logbook.test.PrettyPrintingSink;
import no.entur.logging.cloud.rr.logbook.spring.LogbookLoggingAutoConfiguration;
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
        no.entur.logging.cloud.rr.logbook.spring.LogbookLoggingAutoConfiguration.class,
        LogbookAutoConfiguration.class
})

@Configuration
public class LogbookLoggingTestAutoConfiguration {

    @Value("${entur.logging.request-response.logger.level}")
    protected String loggerLevel;

    @Value("${entur.logging.request-response.logger.name}")
    protected String loggerName;

    @Value("${entur.logging.request-response.max-size}")
    protected int maxSize;

    @Value("${entur.logging.request-response.max-body-size}")
    protected int maxBodySize;

    @Bean
    @ConditionalOnMissingBean(Sink.class)
    public Sink sink() {
        Logger logger = LoggerFactory.getLogger(loggerName);
        Level level = LogbookLoggingAutoConfiguration.parseLevel(loggerLevel);

        Sink machineReadableSink = ConsoleOutputTypeLogLevelLogstashLogbackSink.newBuilder()
                .withLogger(logger)
                .withLogLevel(level)
                .withMaxBodySize(maxBodySize)
                .withMaxSize(maxSize)
                .withValidateRequestJsonBody(true)
                .withValidateResponseJsonBody(false)
                .build();

        Sink humanReadablePlainSink = PrettyPrintingSink.newBuilder().withLogger(logger).withLogLevel(level).build();

        Sink humanReadableJsonSink = PrettyPrintingLogLevelLogstashLogbackSink.newBuilder()
                .withLogger(logger)
                .withLogLevel(level)
                .withMaxBodySize(maxBodySize)
                .withMaxSize(maxSize)
                .withValidateRequestJsonBody(true)
                .withValidateResponseJsonBody(false)
                .build();

        return CompositeSink.newBuilder()
                .withMachineReadableJsonSink(machineReadableSink)
                .withHumanReadablePlainSink(humanReadablePlainSink)
                .withHumanReadableJsonSink(humanReadableJsonSink)
                .build();
    }

}
