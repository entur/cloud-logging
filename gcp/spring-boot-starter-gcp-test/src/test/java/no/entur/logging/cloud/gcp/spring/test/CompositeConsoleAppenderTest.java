package no.entur.logging.cloud.gcp.spring.test;

import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ansi.AnsiOutput;

public class CompositeConsoleAppenderTest {

    private static final DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(CompositeConsoleAppenderTest.class);

    @Test
    public void testHumanReadablePlain() {
        CompositeConsoleAppender.getInstance().useHumanReadablePlainEncoder();

        AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);

        LOGGER.info("Test message");
    }

    @Test
    public void testHumanReadableJson() throws Exception {
        CompositeConsoleAppender.getInstance().useHumanReadableJsonEncoder();

        LOGGER.info("Test info message");
        LOGGER.warn("Test warn message");
        LOGGER.error("Test error message");

        LOGGER.errorTellMeTomorrow("Test error tell me tomorrow message");
        LOGGER.errorInterruptMyDinner("Test error interrupt my dinner message");
        LOGGER.errorWakeMeUpRightNow("Test error wake me up right now message");
    }

    @Test
    public void testMachineReadableJson() throws Exception {
        CompositeConsoleAppender.getInstance().useMachineReadableJsonEncoder();

        LOGGER.info("Test message");
    }

    @AfterEach
    public void waitForFlush()  throws Exception {
        Thread.sleep(100);

        CompositeConsoleAppender.getInstance().useHumanReadablePlainEncoder();
    }

}
