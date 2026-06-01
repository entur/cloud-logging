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
    public void testEachLineIncrementsCounters() {
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

    @Test
    public void testWriteByteArrayIncrementsCounters() {
        double errorBefore = getCount("error");
        double errorTellMeBefore = getCount("errorTellMeTomorrow");

        systemErrMicrometerPrintStream.write("line via write(byte[])\n".getBytes());

        assertThat(getCount("error")).isEqualTo(errorBefore + 1.0);
        assertThat(getCount("errorTellMeTomorrow")).isEqualTo(errorTellMeBefore + 1.0);
    }

    @Test
    @Test
    public void testWriteByteArraySliceIncrementsCounters() {
        double errorBefore = getCount("error");
        double errorTellMeBefore = getCount("errorTellMeTomorrow");

        byte[] bytes = "line via write(byte[],off,len)\n".getBytes();
        systemErrMicrometerPrintStream.write(bytes, 0, bytes.length);

        assertThat(getCount("error")).isEqualTo(errorBefore + 1.0);
        assertThat(getCount("errorTellMeTomorrow")).isEqualTo(errorTellMeBefore + 1.0);
    }

    @Test
    public void testPrintStringWithEmbeddedNewlineIncrementsCounters() {
        double errorBefore = getCount("error");

        System.err.print("first\nsecond\n");

        assertThat(getCount("error")).isEqualTo(errorBefore + 2.0);
    }

    @Test
    public void testPrintlnNoArgsIncrementsCounter() {
        double errorBefore = getCount("error");

        System.err.println();

        assertThat(getCount("error")).isEqualTo(errorBefore + 1.0);
    }

    @Test
    public void testPrintlnBooleanIncrementsCounter() {
        double errorBefore = getCount("error");

        System.err.println(true);

        assertThat(getCount("error")).isEqualTo(errorBefore + 1.0);
    }

    @Test
    public void testPrintlnCharIncrementsCounter() {
        double errorBefore = getCount("error");

        System.err.println('x');

        assertThat(getCount("error")).isEqualTo(errorBefore + 1.0);
    }

    @Test
    public void testPrintlnIntIncrementsCounter() {
        double errorBefore = getCount("error");

        System.err.println(42);

        assertThat(getCount("error")).isEqualTo(errorBefore + 1.0);
    }

    @Test
    public void testPrintlnLongIncrementsCounter() {
        double errorBefore = getCount("error");

        System.err.println(42L);

        assertThat(getCount("error")).isEqualTo(errorBefore + 1.0);
    }

    @Test
    public void testPrintlnFloatIncrementsCounter() {
        double errorBefore = getCount("error");

        System.err.println(3.14f);

        assertThat(getCount("error")).isEqualTo(errorBefore + 1.0);
    }

    @Test
    public void testPrintlnDoubleIncrementsCounter() {
        double errorBefore = getCount("error");

        System.err.println(3.14);

        assertThat(getCount("error")).isEqualTo(errorBefore + 1.0);
    }

    @Test
    public void testPrintlnCharArrayIncrementsCounter() {
        double errorBefore = getCount("error");

        System.err.println(new char[]{'a', 'b', 'c'});

        assertThat(getCount("error")).isEqualTo(errorBefore + 1.0);
    }

    @Test
    public void testPrintlnObjectIncrementsCounter() {
        double errorBefore = getCount("error");

        System.err.println(new Object());

        assertThat(getCount("error")).isEqualTo(errorBefore + 1.0);
    }

    @Test
    public void testFormatIncrementsCounter() {
        double errorBefore = getCount("error");

        System.err.format("formatted %s%n", "message");

        assertThat(getCount("error")).isEqualTo(errorBefore + 1.0);
    }

    @Test
    public void testPrintfIncrementsCounter() {
        double errorBefore = getCount("error");

        System.err.printf("printf %s%n", "message");

        assertThat(getCount("error")).isEqualTo(errorBefore + 1.0);
    }

    private double getCount(String level) {
        Collection<Counter> counters = meterRegistry.find("logback.events").counters();
        Optional<Counter> counter = counters.stream()
                .filter(c -> level.equals(c.getId().getTag("level")))
                .findAny();
        return counter.map(Counter::count).orElse(0.0);
    }
}
