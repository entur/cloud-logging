package no.entur.logging.cloud.rr.logbook.spring.test;


import no.entur.logging.cloud.logbook.LogLevelLogstashLogbackSink;
import no.entur.logging.cloud.logbook.logbook.test.CompositeSink;
import no.entur.logging.cloud.logbook.logbook.test.PrettyPrintingHttpLogFormatter;
import no.entur.logging.cloud.logbook.logbook.test.PrettyPrintingLogLevelLogstashLogbackSink;
import no.entur.logging.cloud.rr.logbook.spring.LogbookLoggingAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.HttpLogFormatter;
import org.zalando.logbook.HttpLogWriter;
import org.zalando.logbook.Sink;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;
import org.zalando.logbook.core.DefaultHttpLogFormatter;
import org.zalando.logbook.core.DefaultHttpLogWriter;
import org.zalando.logbook.core.DefaultSink;

@AutoConfigureBefore(value = {
        no.entur.logging.cloud.rr.logbook.spring.LogbookLoggingAutoConfiguration.class,
        LogbookAutoConfiguration.class
})

@Configuration
public class LogbookLoggingTestAutoConfiguration extends LogbookLoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Sink.class)
    public Sink sink(HttpLogFormatter formatter, final HttpLogWriter writer) {
        Logger logger = LoggerFactory.getLogger(loggerName);
        Level level = LogbookLoggingAutoConfiguration.parseLevel(loggerLevel);

        Sink sink = super.sink();

        Sink humanReadablePlainSink = new DefaultSink(new PrettyPrintingHttpLogFormatter(formatter), writer);

        Sink humanReadableJsonSink = PrettyPrintingLogLevelLogstashLogbackSink.newBuilder()
                .withLogger(logger)
                .withLogLevel(level)
                .withValidateRequestJsonBody(true)
                .withValidateResponseJsonBody(false)
                .build();

        return CompositeSink.newBuilder()
                .withLogger(logger)
                .withLogLevel(level)
                .withValidateRequestJsonBody(true)
                .withValidateResponseJsonBody(false)
                .withMachineReadableJsonSink(sink)
                .withHumanReadablePlainSink(humanReadablePlainSink)
                .withHumanReadableJsonSink(humanReadableJsonSink)
                .build();
    }

}
