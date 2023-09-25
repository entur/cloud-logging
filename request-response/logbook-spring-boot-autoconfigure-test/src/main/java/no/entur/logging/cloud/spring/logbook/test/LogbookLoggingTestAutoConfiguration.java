package no.entur.logging.cloud.spring.logbook.test;


import com.github.skjolber.jackson.jsh.AnsiSyntaxHighlight;
import com.github.skjolber.jackson.jsh.DefaultSyntaxHighlighter;
import no.entur.logging.cloud.logbook.RemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.ondemand.state.RequestHttpMessageStateSupplierSource;
import no.entur.logging.cloud.logbook.ondemand.state.ResponseHttpMessageStateSupplierSource;
import no.entur.logging.cloud.logbook.logbook.test.CompositeSink;
import no.entur.logging.cloud.logbook.logbook.test.PrettyPrintingLogLevelLogstashLogbackSink;
import no.entur.logging.cloud.logbook.logbook.test.PrettyPrintingSink;
import no.entur.logging.cloud.logbook.logbook.test.ondemand.PrettyPrintingOndemandLogLevelLogstashLogbackSink;
import no.entur.logging.cloud.spring.logbook.AbstractLogbookLoggingAutoConfiguration;
import no.entur.logging.cloud.spring.logbook.LogbookLoggingAutoConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.Sink;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;

@AutoConfigureBefore(value = {
        LogbookLoggingAutoConfiguration.class,
        LogbookAutoConfiguration.class
})

@Configuration
public class LogbookLoggingTestAutoConfiguration extends AbstractLogbookLoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(Sink.class)
    @ConditionalOnBean({RequestHttpMessageStateSupplierSource.class, ResponseHttpMessageStateSupplierSource.class, RemoteHttpMessageContextSupplier.class})
    public Sink asyncSink(RequestHttpMessageStateSupplierSource requestHttpMessageStateSupplierSource, ResponseHttpMessageStateSupplierSource responseHttpMessageStateSupplierSource, RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier) {
        Logger logger = LoggerFactory.getLogger(loggerName);
        Level level = LogbookLoggingAutoConfiguration.parseLevel(loggerLevel);

        Sink machineReadableSink = createAsyncMachineReadbleSink(logger, level, requestHttpMessageStateSupplierSource, responseHttpMessageStateSupplierSource, remoteHttpMessageContextSupplier);

        // emulate default intellij color scheme
        DefaultSyntaxHighlighter highlighter = DefaultSyntaxHighlighter.newBuilder()
                .withField(AnsiSyntaxHighlight.MAGENTA)
                .withBoolean(AnsiSyntaxHighlight.BLUE)
                .withNumber(AnsiSyntaxHighlight.BLUE)
                .withString(AnsiSyntaxHighlight.GREEN)
                .build();

        Sink humanReadablePlainSink = new PrettyPrintingSink.Builder()
                .withLogger(logger)
                .withLogLevel(level)
                .withMaxBodySize(maxBodySize)
                .withMaxSize(maxSize)
                .withSyntaxHighlighter(highlighter)
                .withRemoteHttpMessageContextSupplier(remoteHttpMessageContextSupplier)
                .build();

        Sink humanReadableJsonSink = PrettyPrintingOndemandLogLevelLogstashLogbackSink.newBuilder()
                .withLogger(logger)
                .withLogLevel(level)
                .withMaxBodySize(maxBodySize)
                .withMaxSize(maxSize)
                .withValidateRequestJsonBodyWellformed(requestHttpMessageStateSupplierSource)
                .withValidateResponseJsonBodyWellformed(responseHttpMessageStateSupplierSource)
                .withRemoteHttpMessageContextSupplier(remoteHttpMessageContextSupplier)
                .build();

        return CompositeSink.newBuilder()
                .withMachineReadableJsonSink(machineReadableSink)
                .withHumanReadablePlainSink(humanReadablePlainSink)
                .withHumanReadableJsonSink(humanReadableJsonSink)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(Sink.class)
    public Sink sink(RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier ) {
        Logger logger = LoggerFactory.getLogger(loggerName);
        Level level = LogbookLoggingAutoConfiguration.parseLevel(loggerLevel);

        Sink machineReadableSink = createMachineReadbleSink(logger, level, remoteHttpMessageContextSupplier);

        // emulate default intellij color scheme
        DefaultSyntaxHighlighter highlighter = DefaultSyntaxHighlighter.newBuilder()
                .withField(AnsiSyntaxHighlight.MAGENTA)
                .withBoolean(AnsiSyntaxHighlight.BLUE)
                .withNumber(AnsiSyntaxHighlight.BLUE)
                .withString(AnsiSyntaxHighlight.GREEN)
                .build();

        Sink humanReadablePlainSink = new PrettyPrintingSink.Builder()
                .withLogger(logger)
                .withLogLevel(level)
                .withMaxBodySize(maxBodySize)
                .withMaxSize(maxSize)
                .withSyntaxHighlighter(highlighter)
                .withRemoteHttpMessageContextSupplier(remoteHttpMessageContextSupplier)
                .build();

        Sink humanReadableJsonSink = PrettyPrintingLogLevelLogstashLogbackSink.newBuilder()
                .withLogger(logger)
                .withLogLevel(level)
                .withMaxBodySize(maxBodySize)
                .withMaxSize(maxSize)
                .withRemoteHttpMessageContextSupplier(remoteHttpMessageContextSupplier)
                .build();

        return CompositeSink.newBuilder()
                .withMachineReadableJsonSink(machineReadableSink)
                .withHumanReadablePlainSink(humanReadablePlainSink)
                .withHumanReadableJsonSink(humanReadableJsonSink)
                .build();
    }


}
