package no.entur.logging.cloud.logback.logstash.test;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

public class AiFileAppenderTest {

    private static AiFileAppender appender(LoggerContext context, Path file) {
        AiFileAppender appender = new AiFileAppender();
        appender.setContext(context);
        appender.setFile(file.toString());
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%msg%n");
        encoder.start();
        appender.setEncoder(encoder);
        return appender;
    }

    private static boolean posix(Path directory) {
        return directory.getFileSystem().supportedFileAttributeViews().contains("posix");
    }

    private static boolean symbolicLink(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
            return true;
        } catch (IOException | UnsupportedOperationException e) {
            return false;
        }
    }

    @Test
    public void filesAreReadableByTheOwnerOnly(@TempDir Path directory) throws IOException {
        assumeTrue(posix(directory));
        LoggerContext context = new LoggerContext();

        Path created = directory.resolve("new/ai.full");
        AiFileAppender appender = appender(context, created);
        appender.start();
        appender.stop();
        assertThat(Files.getPosixFilePermissions(created)).containsExactly(OWNER_READ, OWNER_WRITE);

        // for example from an earlier version
        Path existing = Files.createFile(directory.resolve("old.full"), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-r--r--")));
        appender = appender(context, existing);
        appender.start();
        appender.stop();
        assertThat(Files.getPosixFilePermissions(existing)).containsExactly(OWNER_READ, OWNER_WRITE);
    }

    @Test
    public void symbolicLinksAreRefused(@TempDir Path directory) {
        Path target = directory.resolve("elsewhere");
        Path link = directory.resolve("ai.full");
        assumeTrue(symbolicLink(link, target));

        AiFileAppender appender = appender(new LoggerContext(), link);
        appender.start();

        assertThat(appender.isStarted()).isFalse();
        assertThat(Files.exists(target)).isFalse();
    }

    @Test
    public void filesOfAnotherUserAreRefused(@TempDir Path directory) throws IOException {
        assumeTrue(posix(directory));
        Path existing = Files.createFile(directory.resolve("ai.full"));
        LoggerContext context = new LoggerContext();
        AiFileAppender appender = new AiFileAppender() {
            @Override
            boolean isOwnedByThisUser(Path path) {
                return false;
            }
        };
        appender.setContext(context);
        appender.setFile(existing.toString());
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%msg%n");
        encoder.start();
        appender.setEncoder(encoder);

        appender.start();

        assertThat(appender.isStarted()).isFalse();
        assertThat(new AiFileAppender().isOwnedByThisUser(existing)).isTrue();
    }

    @Test
    public void lockReachedThroughAnotherPathIsKept(@TempDir Path directory) {
        AiFileAppender appender = new AiFileAppender();
        appender.setContext(new LoggerContext());
        Path lock = directory.resolve("kept.lock");

        assertThat(appender.lock(lock.toString())).isEqualTo(AiFileAppender.LockState.HELD);
        // the same file again (another path in the same JVM): still held, and the second channel is not closed (that could release the lock)
        assertThat(appender.lock(directory.resolve(".").resolve("kept.lock").toString())).isEqualTo(AiFileAppender.LockState.HELD);
    }

    @Test
    public void noAliasesIfLockingFails(@TempDir Path directory) throws IOException {
        AiFileAppender appender = new AiFileAppender();
        appender.setContext(new LoggerContext());

        Path notAFile = Files.createDirectory(directory.resolve("ai.lock"));
        assertThat(appender.lock(notAFile.toString())).isEqualTo(AiFileAppender.LockState.FAILED);

        Path target = directory.resolve("elsewhere");
        Path link = directory.resolve("link.lock");
        if (symbolicLink(link, target)) {
            assertThat(appender.lock(link.toString())).isEqualTo(AiFileAppender.LockState.FAILED);
            assertThat(Files.exists(target)).isFalse();
        }
    }
}
