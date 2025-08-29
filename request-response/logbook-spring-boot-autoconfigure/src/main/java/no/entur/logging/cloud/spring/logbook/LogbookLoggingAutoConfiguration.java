package no.entur.logging.cloud.spring.logbook;

import no.entur.logging.cloud.logbook.DefaultRemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.RemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.ondemand.state.RequestHttpMessageStateSupplierSource;
import no.entur.logging.cloud.logbook.ondemand.state.ResponseHttpMessageStateSupplierSource;
import no.entur.logging.cloud.spring.logbook.properties.FormatProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.Sink;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;
import org.zalando.logbook.autoconfigure.LogbookProperties;

import java.util.List;

@AutoConfigureBefore(value = {
        LogbookAutoConfiguration.class
})

@Configuration
@EnableConfigurationProperties(value = { FormatProperties.class, LogbookProperties.class})
public class LogbookLoggingAutoConfiguration extends AbstractLogbookLoggingAutoConfiguration {

    public LogbookLoggingAutoConfiguration(LogbookProperties properties, @Value("${entur.logging.request-response.logbook.default-excludes:true}") boolean defaultExcludes) {
        // this somewhat of a hack for getting default excludes appended to the app configuration

        if(defaultExcludes) {
            List<String> excludes = properties.getExclude();

            excludes.add("/actuator/health");
            excludes.add("/actuator/health/liveness");
            excludes.add("/actuator/health/readiness");
            excludes.add("/actuator/prometheus");
            excludes.add("/actuator/info");
            excludes.add("/actuator/env");
            excludes.add("/actuator/metrics");
            excludes.add("/actuator/loggers");
            // swagger-related
            excludes.add("/favicon.*");
            excludes.add("/v2/api-docs");
            excludes.add("/v2/api-docs/**");
            excludes.add("/v3/api-docs");
            excludes.add("/v3/api-docs/**");
            excludes.add("/swagger");
            excludes.add("/metrics");
        }
    }

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
