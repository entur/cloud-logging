package no.entur.logging.cloud.spring.logbook;

import no.entur.logging.cloud.logbook.DefaultRemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.RemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.ondemand.state.RequestHttpMessageStateSupplierSource;
import no.entur.logging.cloud.logbook.ondemand.state.ResponseHttpMessageStateSupplierSource;
import no.entur.logging.cloud.spring.logbook.properties.FormatProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.Sink;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;

@AutoConfigureBefore(value = {
        LogbookAutoConfiguration.class
})

@Configuration
@EnableConfigurationProperties(value = { FormatProperties.class })
public class LogbookLoggingAutoConfiguration extends AbstractLogbookLoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RemoteHttpMessageContextSupplier.class)
    public RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier() {
        // by default, verify syntax of all remote JSON payloads
        return new DefaultRemoteHttpMessageContextSupplier();
    }

    @Bean
    @ConditionalOnMissingBean(Sink.class)
    @ConditionalOnBean({ RequestHttpMessageStateSupplierSource.class, ResponseHttpMessageStateSupplierSource.class,
            RemoteHttpMessageContextSupplier.class })
    public Sink asyncSink(RequestHttpMessageStateSupplierSource requestHttpMessageStateSupplierSource,
            ResponseHttpMessageStateSupplierSource responseHttpMessageStateSupplierSource,
            RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier) {
        Logger logger = LoggerFactory.getLogger(loggerName);
        Level level = LogbookLoggingAutoConfiguration.parseLevel(loggerLevel);

        return createAsyncMachineReadableSink(logger, level, requestHttpMessageStateSupplierSource,
                responseHttpMessageStateSupplierSource, remoteHttpMessageContextSupplier);
    }

    // ignore HttpLogFormatter and HttpLogWriter
    @Bean
    @ConditionalOnMissingBean(Sink.class)
    public Sink sink(RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier) {
        Logger logger = LoggerFactory.getLogger(loggerName);
        Level level = parseLevel(loggerLevel);

        // externalized decision on whether to trust incoming JSON is well-formed
        // for example an authorized client could be trusted

        return createMachineReadbleSink(logger, level, remoteHttpMessageContextSupplier);
    }
}
