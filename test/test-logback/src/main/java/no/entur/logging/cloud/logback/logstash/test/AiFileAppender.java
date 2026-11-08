package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File appender for AI mode. All test JVMs (one per module) append to the same file, so the first time a JVM opens the file a line
 * <code>---- JVM pid 123 started ... in /path/to/module (aliases u1, h1, ... restart) ----</code> is written, marking where the
 * output of a new JVM starts and where aliases begin to refer to other values. Logback may be reconfigured several times per JVM
 * (for example by Spring Boot), the line is written once per JVM.
 */
public class AiFileAppender extends FileAppender<ILoggingEvent> {

    private static final Set<String> STARTED = ConcurrentHashMap.newKeySet();

    @Override
    public void start() {
        super.start();
        if (isStarted() && STARTED.add(getFile())) {
            String line = "---- JVM pid " + ProcessHandle.current().pid()
                    + " started " + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                    + " in " + System.getProperty("user.dir")
                    + " (aliases u1, h1, ... restart) ----" + System.lineSeparator();
            streamWriteLock.lock();
            try {
                writeByteArrayToOutputStreamWithPossibleFlush(line.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                addError("Unable to write JVM start line to " + getFile(), e);
            } finally {
                streamWriteLock.unlock();
            }
        }
    }
}
