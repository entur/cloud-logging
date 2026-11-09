package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * File appender for AI mode. All test JVMs (one per module) append to the same file, so the first time a JVM opens the file a line
 * <code>---- JVM pid 123 started ... in /path/to/module (aliases u1, h1, ... restart) ----</code> is written, marking where the
 * output of a new JVM starts and where aliases begin to refer to other values. Logback may be reconfigured several times per JVM
 * (for example by Spring Boot), the line is written once per JVM.
 * <p>
 * Test JVMs may run at the same time (Gradle <code>--parallel</code>, <code>maxParallelForks</code>, Maven <code>forkCount</code>), and then
 * their events are interleaved. The JVM holding the lock file (see {@link #setLockFile(String)}) uses aliases; the others leave UUIDs and
 * hex ids as they are, and their line says <code>(another JVM is writing to this file, no aliases)</code>. So an alias always belongs to the
 * JVM of the last line before it that says <code>aliases ... restart</code>. If the lock file cannot be locked, this JVM does not use
 * aliases either.
 * <p>
 * The files hold whole request and response bodies and are often in a shared temporary directory, so they are created readable by the
 * owner only (where the file system has POSIX permissions), and a file that is a symbolic link or belongs to another user is refused.
 */
public class AiFileAppender extends FileAppender<ILoggingEvent> {

    enum LockState { HELD, OTHER_JVM, FAILED }

    private static final Set<PosixFilePermission> OWNER_ONLY = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    private static final Set<String> STARTED = ConcurrentHashMap.newKeySet();

    // whether this JVM got the lock, per lock file
    private static final Map<String, LockState> LOCKED = new ConcurrentHashMap<>();
    // referenced until the JVM exits: an unreachable channel is closed by the garbage collector, which releases the lock
    private static final Map<String, FileLock> LOCKS = new ConcurrentHashMap<>();
    // channels opened on a lock file this JVM had already locked through another path: closing them could release that lock
    private static final List<FileChannel> KEPT_OPEN = new CopyOnWriteArrayList<>();

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
        if (getFile() != null && !createOwnerOnly(Path.of(getFile()))) {
            return;
        }
        super.start();
        if (!isStarted()) {
            return;
        }
        AiLoggingControl.aiFileStarted();
        LockState lock = lockFile == null ? LockState.HELD : LOCKED.computeIfAbsent(normalize(lockFile), this::lock);
        if (lock != LockState.HELD) {
            AiLoggingControl.disableAliasesInThisJvm();
        }
        if (STARTED.add(getFile())) {
            String line = "---- JVM pid " + ProcessHandle.current().pid()
                    + " started " + LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                    + " in " + System.getProperty("user.dir")
                    + switch (lock) {
                        case HELD -> " (aliases u1, h1, ... restart) ----";
                        case OTHER_JVM -> " (another JVM is writing to this file, no aliases) ----";
                        case FAILED -> " (could not lock " + lockFile + ", no aliases) ----";
                    }
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
        boolean posix = path.getFileSystem().supportedFileAttributeViews().contains("posix");
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (posix) {
                Files.createFile(path, PosixFilePermissions.asFileAttribute(OWNER_ONLY));
            } else {
                Files.createFile(path);
            }
            return true;
        } catch (FileAlreadyExistsException e) {
            // also if it is a symbolic link, even one to a missing file
        } catch (IOException | RuntimeException e) {
            addError("Unable to create " + path, e);
            return false;
        }
        if (Files.isSymbolicLink(path)) {
            addError("Not writing to " + path + ": it is a symbolic link");
            return false;
        }
        if (posix && !isOwnedByThisUser(path)) {
            addError("Not writing to " + path + ": it belongs to another user");
            return false;
        }
        if (posix && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            try {
                Files.setPosixFilePermissions(path, OWNER_ONLY);
            } catch (IOException | RuntimeException e) {
                // not the owner
            }
        }
        return true;
    }

    /** True if the file belongs to the user running this JVM, or if that cannot be told. */
    boolean isOwnedByThisUser(Path path) {
        try {
            UserPrincipal owner = Files.getOwner(path, LinkOption.NOFOLLOW_LINKS);
            UserPrincipal user = path.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name"));
            return owner.equals(user);
        } catch (IOException | RuntimeException e) {
            return true;
        }
    }

    /** Whether this JVM got the lock, another JVM holds it, or locking failed (then aliases are off, as for another JVM). */
    LockState lock(String file) {
        FileChannel channel = null;
        try {
            Path path = Path.of(file);
            if (!createOwnerOnly(path)) {
                return LockState.FAILED;
            }
            channel = FileChannel.open(path, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
            FileLock lock = channel.tryLock();
            if (lock != null) {
                LOCKS.put(file, lock);
                channel = null; // keep open
                return LockState.HELD;
            }
            return LockState.OTHER_JVM;
        } catch (OverlappingFileLockException e) {
            // already held by this JVM (through another path); closing this channel could release that lock
            if (channel != null) {
                KEPT_OPEN.add(channel);
                channel = null;
            }
            return LockState.HELD;
        } catch (IOException | RuntimeException e) {
            addWarn("Unable to lock " + file + ", so not using aliases in " + getFile(), e);
            return LockState.FAILED;
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
