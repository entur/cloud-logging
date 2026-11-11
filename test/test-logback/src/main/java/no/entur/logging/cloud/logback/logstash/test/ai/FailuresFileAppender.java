package no.entur.logging.cloud.logback.logstash.test.ai;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * AI mode: the failures file, <code>&lt;file&gt;.failures</code>. Keeps the WARN/ERROR events (rendered with its encoder, as in the compact
 * file but always with the <code>#id</code>) in {@link AiTestRun} instead of writing them, and when a test plan has finished
 * ({@link AiTestExecutionListener}) writes the report: per failed test, the events logged while it ran. Like the other AI files it is
 * readable by the owner only and starts with a line per JVM (see {@link AiFileAppender}).
 */
public class FailuresFileAppender extends AiFileAppender {

    private static volatile FailuresFileAppender current;

    @Override
    public void start() {
        super.start();
        if (isStarted()) {
            // logback may be reconfigured (for example per Spring context): the newest appender writes the report
            current = this;
        }
    }

    @Override
    public void stop() {
        if (current == this) {
            current = null;
        }
        super.stop();
    }

    @Override
    protected void subAppend(ILoggingEvent event) {
        if (!isStarted() || !event.getLevel().isGreaterOrEqual(Level.WARN) || !AiLoggingControl.isFailuresEnabled()) {
            return;
        }
        String line = new String(getEncoder().encode(event), StandardCharsets.UTF_8).strip();
        AiTestRun.current().event(event.getInstant(), event.getThreadName(), simpleName(event.getLoggerName()), event.getLevel().toString(), line);
    }

    /** Writes the report of the run to the failures file of the current configuration, then forgets the run; nothing without AI files. */
    static void writeReport(AiTestRun run) {
        FailuresFileAppender appender = current;
        if (appender == null) {
            return;
        }
        appender.write(run.report());
        run.clear();
    }

    private void write(String report) {
        streamWriteLock.lock();
        try {
            writeByteArrayToOutputStreamWithPossibleFlush(report.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            addError("Unable to write the failures report to " + getFile(), e);
        } finally {
            streamWriteLock.unlock();
        }
    }

    /** The simple class name if the logger is named after a class, as in the compact file. */
    static String simpleName(String loggerName) {
        if (loggerName == null) {
            return "";
        }
        int index = loggerName.lastIndexOf('.');
        if (index == -1 || index == loggerName.length() - 1 || !Character.isUpperCase(loggerName.charAt(index + 1))) {
            return loggerName;
        }
        return loggerName.substring(index + 1);
    }
}
