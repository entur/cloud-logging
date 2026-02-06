package no.entur.logging.cloud.gcp.spring.web;

import net.logstash.logback.marker.MapEntriesAppendingMarker;
import no.entur.logging.cloud.api.DevOpsLogger;
import no.entur.logging.cloud.api.DevOpsLoggerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.util.Map;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@DirtiesContext
@EnableAutoConfiguration
public class LoadContextLoggingTest {

    private static final DevOpsLogger LOGGER = DevOpsLoggerFactory.getLogger(LoadContextLoggingTest.class);

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
    public void testMachineReadableJsonWithException() throws IOException {
        IOException e = new IOException("Something went wrong");

        LOGGER.trace("Test trace message with exception", e);
        LOGGER.debug("Test debug message with exception", e);
        LOGGER.info("Test info message with exception", e);
        LOGGER.warn("Test warn message with exception", e);
        LOGGER.error("Test error message with exception", e);

        LOGGER.errorTellMeTomorrow("Test error tell me tomorrow message with exception", e);
        LOGGER.errorInterruptMyDinner("Test error interrupt my dinner message with exception", e);
        LOGGER.errorWakeMeUpRightNow("Test error wake me up right now message with exception", e);
    }

    @Test
    public void testMachineReadableJsonMarkerFirst() throws IOException {
        Map<String, String> map = Map.of(
                "class", "myErrorClass",
                "message", "myMessage",
                "responseCode", "myResponseCode",
                "responseSource", "myResponseSource",
                "responseText", "myResponseText");

        LOGGER.trace(new MapEntriesAppendingMarker(map), "Test trace message");
        LOGGER.debug(new MapEntriesAppendingMarker(map), "Test debug message");
        LOGGER.info(new MapEntriesAppendingMarker(map), "Test info message");
        LOGGER.warn(new MapEntriesAppendingMarker(map), "Test warn message");
        LOGGER.error(new MapEntriesAppendingMarker(map), "Test error message");

        LOGGER.errorTellMeTomorrow(new MapEntriesAppendingMarker(map), "Test error tell me tomorrow message");
        LOGGER.errorInterruptMyDinner(new MapEntriesAppendingMarker(map), "Test error interrupt my dinner message");
        LOGGER.errorWakeMeUpRightNow(new MapEntriesAppendingMarker(map), "Test error wake me up right now message");
    }

    @Test
    public void testMachineReadableJsonMarkerLast() throws IOException {
        Map<String, String> map = Map.of(
                "class", "myErrorClass",
                "message", "myMessage",
                "responseCode", "myResponseCode",
                "responseSource", "myResponseSource",
                "responseText", "myResponseText");

        LOGGER.trace("Test trace message with exception", new MapEntriesAppendingMarker(map));
        LOGGER.debug("Test debug message with exception", new MapEntriesAppendingMarker(map));
        LOGGER.info("Test info message with exception", new MapEntriesAppendingMarker(map));
        LOGGER.warn("Test warn message with exception", new MapEntriesAppendingMarker(map));
        LOGGER.error("Test error message with exception", new MapEntriesAppendingMarker(map));

        LOGGER.errorTellMeTomorrow("Test error tell me tomorrow message with exception", new MapEntriesAppendingMarker(map));
        LOGGER.errorInterruptMyDinner("Test error interrupt my dinner message with exception", new MapEntriesAppendingMarker(map));
        LOGGER.errorWakeMeUpRightNow("Test error wake me up right now message with exception", new MapEntriesAppendingMarker(map));
    }

}
