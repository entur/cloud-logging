package no.entur.logging.cloud.spring.stderr.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that System.err is intercepted and Micrometer counters are incremented when
 * the autoconfiguration is enabled (the default behaviour).
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@DirtiesContext
public class StderrMicrometerEnabledTest {

    @Autowired
    private SystemErrMicrometerPrintStream systemErrMicrometerPrintStream;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    public void testBeanCreated() {
        assertThat(systemErrMicrometerPrintStream).isNotNull();
    }

    @Test
    public void testSystemErrIsIntercepted() {
        assertThat(System.err).isSameInstanceAs(systemErrMicrometerPrintStream);
    }

    @Test
    public void testPrintlnIncrementsCounters() {
        double errorBefore = getCount("error");
        double errorTellMeBefore = getCount("errorTellMeTomorrow");

        System.err.println("Test error line");

        assertThat(getCount("error")).isEqualTo(errorBefore + 1.0);
        assertThat(getCount("errorTellMeTomorrow")).isEqualTo(errorTellMeBefore + 1.0);
    }

    @Test
    public void testMultipleLinesIncrementCountersOnce() {
        double errorBefore = getCount("error");

        System.err.println("line one");
        System.err.println("line two");
        System.err.println("line three");

        assertThat(getCount("error")).isEqualTo(errorBefore + 3.0);
    }

    @Test
    public void testWriteWithoutNewlineDoesNotIncrementCounters() {
        double errorBefore = getCount("error");

        System.err.print("no newline here");

        assertThat(getCount("error")).isEqualTo(errorBefore);
    }

    private double getCount(String level) {
        Collection<Counter> counters = meterRegistry.find("logback.events").counters();
        Optional<Counter> counter = counters.stream()
                .filter(c -> level.equals(c.getId().getTag("level")))
                .findAny();
        return counter.map(Counter::count).orElse(0.0);
    }
}
