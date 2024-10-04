package no.entur.logging.cloud.azure.spring.web;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@DirtiesContext
@EnableAutoConfiguration
@TestPropertySource(properties = {
        "entur.logging.http.ondemand.enabled=true",
        "entur.logging.http.ondemand.failure.http.statusCode.equalOrHigherThan=400",

})
public class LoadOndemandContextLoggingTest {

    private static final DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(LoadOndemandContextLoggingTest.class);

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
    public void testAppender() throws IOException, InterruptedException {
        LoggingScopeAsyncAppender appender = getOndemandAsyncAppender();

        assertTrue(appender.getScopeProviders().isEmpty());
    }

    private static LoggingScopeAsyncAppender getOndemandAsyncAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders();
        if(!appenderIterator.hasNext()) {
            throw new RuntimeException("No appenders configured");
        }
        while (appenderIterator.hasNext()) {
            Appender<ILoggingEvent> appender = appenderIterator.next();
            if(appender instanceof LoggingScopeAsyncAppender) {
                return (LoggingScopeAsyncAppender) appender;
            }
        }
        throw new RuntimeException();
    }


}
