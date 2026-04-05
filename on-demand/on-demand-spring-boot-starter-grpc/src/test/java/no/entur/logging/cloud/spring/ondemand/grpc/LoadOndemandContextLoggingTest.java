package no.entur.logging.cloud.spring.ondemand.grpc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static com.google.common.truth.Truth.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
@EnableAutoConfiguration
@TestPropertySource(properties = {
        "spring.grpc.server.port=-1",
        "entur.logging.grpc.ondemand.enabled=true"
})
public class LoadOndemandContextLoggingTest {

    private static final DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(LoadOndemandContextLoggingTest.class);

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    public void setupAppender() {
        Logger logbackLogger = (Logger) LoggerFactory.getLogger(LoadOndemandContextLoggingTest.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logbackLogger.addAppender(listAppender);
    }

    @AfterEach
    public void teardownAppender() {
        Logger logbackLogger = (Logger) LoggerFactory.getLogger(LoadOndemandContextLoggingTest.class);
        logbackLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    public void testContextLoads() {}

    @Test
    public void testLoggingAtAllLevels() {
        LOGGER.info("Test info");
        LOGGER.warn("Test warn");
        LOGGER.error("Test error");

        assertThat(listAppender.list).isNotEmpty();
        assertThat(listAppender.list.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(listAppender.list.get(1).getLevel()).isEqualTo(Level.WARN);
        assertThat(listAppender.list.get(2).getLevel()).isEqualTo(Level.ERROR);
    }
}
