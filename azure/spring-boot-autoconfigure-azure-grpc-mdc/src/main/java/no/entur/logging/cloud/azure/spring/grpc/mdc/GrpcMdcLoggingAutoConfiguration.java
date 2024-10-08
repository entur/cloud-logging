package no.entur.logging.cloud.azure.spring.grpc.mdc;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import no.entur.logging.cloud.appender.MdcAsyncAppender;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContributer;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Iterator;

@Configuration
public class GrpcMdcLoggingAutoConfiguration {

    @Bean
    public MdcAsyncAppender mdcAsyncAppender() {
        MdcAsyncAppender appender = getAppender();

        appender.setMdcContributer(new GrpcMdcContributer());

        return appender;
    }

    private static MdcAsyncAppender getAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders();
        if(!appenderIterator.hasNext()) {
            throw new IllegalStateException("Unexpected log appenders configured, expected at least one which is implementing " + MdcAsyncAppender.class.getName());
        }
        while (appenderIterator.hasNext()) {
            Appender<ILoggingEvent> appender = appenderIterator.next();
            if (appender instanceof MdcAsyncAppender) {
                return (MdcAsyncAppender) appender;
            }
        }
        throw new IllegalStateException("Unexpected log appenders configured, expected one implementing " + MdcAsyncAppender.class.getName());
    }

}
