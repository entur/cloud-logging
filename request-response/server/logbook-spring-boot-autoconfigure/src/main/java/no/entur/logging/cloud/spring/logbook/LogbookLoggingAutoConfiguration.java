package no.entur.logging.cloud.spring.logbook;

import no.entur.logging.cloud.logbook.ValidateWellformedRequestBodyDecisionSupplier;
import no.entur.logging.cloud.logbook.ValidateWellformedResponseBodyDecisionSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
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
public class LogbookLoggingAutoConfiguration extends AbstractLogbookLoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ValidateWellformedRequestBodyDecisionSupplier.class)
    public ValidateWellformedRequestBodyDecisionSupplier validateWellformedRequestBodyDecisionSupplier() {
        return () -> () -> false;
    }

    @Bean
    @ConditionalOnMissingBean(ValidateWellformedResponseBodyDecisionSupplier.class)
    public ValidateWellformedResponseBodyDecisionSupplier validateWellformedResponseBodyDecisionSupplier() {
        // assume we always output valid JSON
        return () -> () -> true;
    }

    // ignore HttpLogFormatter and HttpLogWriter
    @Bean
    @ConditionalOnMissingBean(Sink.class)
    public Sink sink(ValidateWellformedRequestBodyDecisionSupplier validateWellformedRequestBodyDecisionSupplier, ValidateWellformedResponseBodyDecisionSupplier validateWellformedResponseBodyDecisionSupplier) {
        Logger logger = LoggerFactory.getLogger(loggerName);
        Level level = parseLevel(loggerLevel);

        // externalized decision on whether to trust incoming JSON is well-formed
        // for example an authorized client could be trusted

        return createMachineReadbleSink(logger, level, validateWellformedRequestBodyDecisionSupplier, validateWellformedResponseBodyDecisionSupplier);
    }
}
