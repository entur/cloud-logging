package no.entur.logging.cloud.gcp.spring.web.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import no.entur.logging.cloud.appender.scope.ScopeAsyncAppender;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.appender.scope.LoggingScopeFactory;
import no.entur.logging.cloud.appender.scope.predicate.LowerOrEqualToLogLevelPredicate;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Predicate;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@DirtiesContext
@EnableAutoConfiguration
@TestPropertySource(properties = "entur.logging.http.ondemand.enabled=true")
public class LoadOndemandContextLoggingTest {

    private static final DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(LoadOndemandContextLoggingTest.class);

    @Value("${entur.logging.http.ondemand.enabled}")
    private boolean enabled;

    @Test
    public void testMachineReadableJson() throws IOException {
        LOGGER.trace("Test trace message");
        LOGGER.debug("Test debug message");
        LOGGER.info("Test info message");
        LOGGER.warn("Test warn message");
        LOGGER.error("Test error message");

        LOGGER.errorTellMeTomorrow("Test error tell me tomorrow message");
        LOGGER.errorInterruptMyDinner("Test error interrupt my dinner message");
        LOGGER.errorWakeMeUpRightNow("Test error wake me up right now message");
    }

    @Test
    public void testOndemandMachineReadableJson() throws IOException, InterruptedException {
        ScopeAsyncAppender appender = getOndemandAsyncAppender();

        LoggingScopeFactory loggingScopeFactory = (LoggingScopeFactory) appender.getScopeProviders().get(0);

        Predicate<ILoggingEvent> queuePredicate = new LowerOrEqualToLogLevelPredicate(Level.INFO_INT);
        Predicate<ILoggingEvent> ignorePredicate = new LowerOrEqualToLogLevelPredicate(Level.DEBUG_INT);

        LoggingScope scope = (LoggingScope) loggingScopeFactory.openScope(queuePredicate, ignorePredicate);

        LOGGER.trace("Test trace message, this should be ignored");
        LOGGER.debug("Test debug message, this should be ignored");
        LOGGER.info("Test info message, this message should be delayed");
        LOGGER.warn("Test warn message, this message should printet at once");
        LOGGER.error("Test error message, this message should printet at once");

        LOGGER.errorTellMeTomorrow("Test error tell me tomorrow message, this message should printet at once");
        LOGGER.errorInterruptMyDinner("Test error interrupt my dinner message, this message should printet at once");
        LOGGER.errorWakeMeUpRightNow("Test error wake me up right now message, this message should printet at once");

        Thread.sleep(1);

        System.out.println("Before flush");
        Thread.sleep(5000);
        appender.write(scope);
        System.out.println("After flush");
        loggingScopeFactory.closeScope(scope);
    }

    private static ScopeAsyncAppender getOndemandAsyncAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders();
        if(!appenderIterator.hasNext()) {
            throw new RuntimeException("No appenders configured");
        }
        while (appenderIterator.hasNext()) {
            Appender<ILoggingEvent> appender = appenderIterator.next();
            if(appender instanceof ScopeAsyncAppender) {
                return (ScopeAsyncAppender) appender;
            }
        }
        throw new RuntimeException();
    }


}
