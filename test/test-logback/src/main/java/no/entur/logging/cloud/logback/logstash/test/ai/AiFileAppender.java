package no.entur.logging.cloud.logback.logstash.test.ai;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File appender for AI mode. Every test JVM writes its own files (the pid is part of the file name, see {@link AiPidPropertyDefiner}),
 * so test JVMs running at the same time (Gradle <code>--parallel</code>, <code>maxParallelForks</code>, Maven <code>forkCount</code>)
 * do not write into each other's lines, and aliases and test tokens are unique within a file. The first time the file is opened a line
 * <code>---- JVM pid 123 started ... in /path/to/module ----</code> is written; logback may be reconfigured several times per JVM
 * (for example by Spring Boot), the line is written once.
 * <p>
 * The files hold whole request and response bodies and are often in a shared temporary directory, so they are created readable by the
 * owner only (where the file system has POSIX permissions), and a file that is a symbolic link or belongs to another user is refused.
 */
public class AiFileAppender extends FileAppender<ILoggingEvent> {

    private static final Set<String> STARTED = ConcurrentHashMap.newKeySet();

    // start lines of tests which have not logged anything yet, per file; kept when logging is reconfigured (for example by Spring Boot) during a test
    private static final Map<String, Map<String, String>> PENDING_TEST_STARTS = new ConcurrentHashMap<>();

    private boolean skipEmptyTests;

    private volatile Map<String, String> pendingTestStarts;

    /** Whether to leave out the marker lines of tests which passed without logging anything (compact file). Default false. */
    public void setSkipEmptyTests(boolean skipEmptyTests) {
        this.skipEmptyTests = skipEmptyTests;
    }

    public boolean isSkipEmptyTests() {
        return skipEmptyTests;
    }

    @Override
    public void start() {
        if (getFile() != null && !createOwnerOnly(Path.of(getFile()))) {
            return;
        }
        super.start();
        if (!isStarted()) {
            return;
        }
        AiLoggingControl.aiFileStarted();
        if (STARTED.add(getFile())) {
            String line = "---- JVM pid " + ProcessHandle.current().pid()
                    + " started " + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                    + " in " + System.getProperty("user.dir") + " ----" + System.lineSeparator();
            writeLine(line);
        }
        pendingTestStarts = skipEmptyTests ? PENDING_TEST_STARTS.computeIfAbsent(normalize(getFile()), file -> Collections.synchronizedMap(new LinkedHashMap<>())) : null;
    }

    /**
     * Test markers (see {@link AiTestMarker}) are written as they are, whatever the filters and the pattern of this appender. With
     * {@link #setSkipEmptyTests(boolean)}, the start line of a test is held back until an event is written, and a test which passed
     * without any event leaves no lines.
     */
    @Override
    public void doAppend(ILoggingEvent event) {
        AiTestMarker marker = AiTestMarker.of(event);
        if (marker == null) {
            super.doAppend(event);
        } else if (isStarted()) {
            testMarker(marker);
        }
    }

    private void testMarker(AiTestMarker marker) {
        Map<String, String> pending = pendingTestStarts;
        if (marker.isStart()) {
            if (pending != null) {
                pending.put(marker.getToken(), marker.getLine());
            } else {
                writeLine(marker.getLine() + System.lineSeparator());
            }
            return;
        }
        String start = pending == null ? null : pending.remove(marker.getToken());
        if (start != null) {
            if (!marker.isFailed()) {
                return; // nothing was logged during this test
            }
            writeLine(start + System.lineSeparator());
        }
        writeLine(marker.getLine() + System.lineSeparator());
    }

    @Override
    protected void subAppend(ILoggingEvent event) {
        Map<String, String> pending = pendingTestStarts;
        if (pending != null && !pending.isEmpty()) {
            synchronized (pending) {
                for (String line : pending.values()) {
                    writeLine(line + System.lineSeparator());
                }
                pending.clear();
            }
        }
        super.subAppend(event);
    }

    private void writeLine(String line) {
        streamWriteLock.lock();
        try {
            writeByteArrayToOutputStreamWithPossibleFlush(line.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            addError("Unable to write to " + getFile(), e);
        } finally {
            streamWriteLock.unlock();
        }
    }

    private static String normalize(String file) {
        try {
            return Path.of(file).toAbsolutePath().normalize().toString();
        } catch (RuntimeException e) {
            return file;
        }
    }

    /**
     * Creates the file readable by the owner only, or makes an existing file of this user so. Returns false, with an error, if the file is
     * a symbolic link, belongs to another user (it could be readable by others) or cannot be created.
     */
    boolean createOwnerOnly(Path path) {
        try {
            AiFiles.createOwnerOnly(path, this::isOwnedByThisUser);
            return true;
        } catch (IOException e) {
            if (e.getCause() != null) {
                addError(e.getMessage(), e.getCause());
            } else {
                addError(e.getMessage());
            }
            return false;
        }
    }

    /** True if the file belongs to the user running this JVM, or if that cannot be told. */
    boolean isOwnedByThisUser(Path path) {
        return AiFiles.isOwnedByThisUser(path);
    }
}
