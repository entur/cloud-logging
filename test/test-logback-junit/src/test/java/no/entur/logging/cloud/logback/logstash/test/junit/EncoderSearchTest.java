package no.entur.logging.cloud.logback.logstash.test.junit;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import no.entur.logging.cloud.logback.logstash.test.NoopAppender;

public class EncoderSearchTest {

    @Test
    public void onlyTheConsoleAndTheAiModeEncoderAppenderAreSearched() {
        LogstashEncoder encoder = new LogstashEncoder();
        LogbackTestExtension extension = new LogbackTestExtension();

        // for example a project's JSON log file: its encoder may have other field names than the console's
        FileAppender<ILoggingEvent> file = new FileAppender<>();
        file.setEncoder(encoder);
        assertThat(extension.searchCompositeJsonEncoder(List.<Appender<ILoggingEvent>>of(file).iterator())).isNull();

        ConsoleAppender<ILoggingEvent> console = new ConsoleAppender<>();
        console.setEncoder(encoder);
        assertThat(extension.searchCompositeJsonEncoder(List.<Appender<ILoggingEvent>>of(file, console).iterator())).isSameInstanceAs(encoder);

        NoopAppender aiMode = new NoopAppender();
        aiMode.setEncoder(encoder);
        assertThat(extension.searchCompositeJsonEncoder(List.<Appender<ILoggingEvent>>of(aiMode).iterator())).isSameInstanceAs(encoder);
    }
}
