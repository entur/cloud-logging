package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File appender for AI mode. All test JVMs (one per module) append to the same file, so the first time a JVM opens the file a line
 * <code>---- JVM pid 123 started ... in /path/to/module (aliases u1, h1, ... restart) ----</code> is written, marking where the
 * output of a new JVM starts and where aliases begin to refer to other values. Logback may be reconfigured several times per JVM
 * (for example by Spring Boot), the line is written once per JVM.
 * <p>
 * Test JVMs may run at the same time (Gradle <code>--parallel</code>, <code>maxParallelForks</code>, Maven <code>forkCount</code>), and then
 * their events are interleaved. The JVM holding the lock file (see {@link #setLockFile(String)}) uses aliases; the others leave UUIDs and
 * hex ids as they are, and their line says <code>(another JVM is writing to this file, no aliases)</code>. So an alias always belongs to the
 * JVM of the last line before it that says <code>aliases ... restart</code>.
 */
public class AiFileAppender extends FileAppender<ILoggingEvent> {

    private static final Set<String> STARTED = ConcurrentHashMap.newKeySet();

    // whether this JVM got the lock, per lock file
    private static final Map<String, Boolean> LOCKED = new ConcurrentHashMap<>();
    // referenced until the JVM exits: an unreachable channel is closed by the garbage collector, which releases the lock
    private static final Map<String, FileLock> LOCKS = new ConcurrentHashMap<>();

    private String lockFile;

    /** File locked by the test JVM which uses aliases. Without it, all JVMs use aliases. */
    public void setLockFile(String lockFile) {
        this.lockFile = lockFile;
    }

    public String getLockFile() {
        return lockFile;
    }

    @Override
    public void start() {
        super.start();
        if (!isStarted()) {
            return;
        }
        boolean alone = lockFile == null || LOCKED.computeIfAbsent(lockFile, this::lock);
        if (!alone) {
            AiLoggingControl.disableAliasesInThisJvm();
        }
        if (STARTED.add(getFile())) {
            String line = "---- JVM pid " + ProcessHandle.current().pid()
                    + " started " + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                    + " in " + System.getProperty("user.dir")
                    + (alone ? " (aliases u1, h1, ... restart) ----" : " (another JVM is writing to this file, no aliases) ----")
                    + System.lineSeparator();
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

    /** Returns true if this JVM got the lock, false if another JVM holds it. */
    private boolean lock(String file) {
        FileChannel channel = null;
        try {
            channel = FileChannel.open(Path.of(file), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock lock = channel.tryLock();
            if (lock != null) {
                LOCKS.put(file, lock);
                channel = null; // keep open
                return true;
            }
            return false;
        } catch (OverlappingFileLockException e) {
            // already held by this JVM
            return true;
        } catch (IOException | RuntimeException e) {
            addWarn("Unable to lock " + file + ", assuming no other test JVM writes to " + getFile(), e);
            return true;
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
