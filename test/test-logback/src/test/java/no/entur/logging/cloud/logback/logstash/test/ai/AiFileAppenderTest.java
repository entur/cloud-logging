package no.entur.logging.cloud.logback.logstash.test.ai;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.LoggingEvent;

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

    @Test
    public void jvmStartLineIsWrittenOncePerFile(@TempDir Path directory) throws Exception {
        File file = directory.resolve("ai.log").toFile();
        LoggerContext context = new LoggerContext();
        for (int i = 0; i < 2; i++) {
            AiFileAppender appender = appender(context, file.toPath());
            appender.start();
            LoggingEvent event = new LoggingEvent("f", context.getLogger("x"), Level.INFO, "message", null, null);
            event.setMDCPropertyMap(Map.of());
            appender.doAppend(event);
            appender.stop();
        }

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).matches("---- JVM pid \\d+ started .* in .* \\(aliases u1, h1, \\.\\.\\. restart\\) ----");
        assertThat(lines.subList(1, 3)).containsExactly("message", "message");
    }
}
