package no.entur.logging.cloud.azure.spring.grpc.ecosystem.test;

import no.entur.logging.cloud.spring.rr.grpc.AbstractRequestResponseGrpcSinkAutoConfiguration;
import no.entur.logging.cloud.rr.grpc.GrpcSink;
import no.entur.logging.cloud.rr.grpc.test.CompositeSink;
import no.entur.logging.cloud.rr.grpc.test.PrettyPrintingLogLevelLogstashLogbackGrpcSink;
import no.entur.logging.cloud.rr.grpc.test.PrettyPrintingGrpcSink;
import org.entur.jackson.tools.jsh.AnsiSyntaxHighlight;
import org.entur.jackson.tools.jsh.DefaultSyntaxHighlighter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@AutoConfigureBefore(value = {
        no.entur.logging.cloud.azure.spring.grpc.ecosystem.RequestResponseAzureGrpcEcosystemAutoConfiguration.class,
})

@Configuration
public class RequestResponseAzureGrpcEcosystemTestAutoConfiguration extends AbstractRequestResponseGrpcSinkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(GrpcSink.class)
    public GrpcSink grpcSink() {
        Logger logger = LoggerFactory.getLogger(loggerName);
        Level level = parseLevel(loggerLevel);

        GrpcSink machineReadableSink = createMachineReadbleSink(logger, level);

        // emulate default intellij color scheme
        DefaultSyntaxHighlighter highlighter = DefaultSyntaxHighlighter.newBuilder()
                .withField(AnsiSyntaxHighlight.MAGENTA)
                .withBoolean(AnsiSyntaxHighlight.BLUE)
                .withNumber(AnsiSyntaxHighlight.BLUE)
                .withString(AnsiSyntaxHighlight.GREEN)
                .build();

        GrpcSink humanReadablePlainSink = new PrettyPrintingGrpcSink.Builder()
                .withLogger(logger)
                .withLogLevel(level)
                .withSyntaxHighlighter(highlighter)
                .build();

        GrpcSink humanReadableJsonSink = PrettyPrintingLogLevelLogstashLogbackGrpcSink.newBuilder()
                .withLogger(logger)
                .withLogLevel(level)
                .build();

        return CompositeSink.newBuilder()
                .withMachineReadableJsonSink(machineReadableSink)
                .withHumanReadablePlainSink(humanReadablePlainSink)
                .withHumanReadableJsonSink(humanReadableJsonSink)
                .build();
    }

}
