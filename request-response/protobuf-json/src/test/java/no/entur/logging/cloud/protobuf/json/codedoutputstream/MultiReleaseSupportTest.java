package no.entur.logging.cloud.protobuf.json.codedoutputstream;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

public class MultiReleaseSupportTest {

    @Test
    public void explainsOldJavaVersion() {
        assertThat(MultiReleaseSupport.unavailableCause(21)).hasMessageThat().isEqualTo("Requires Java 24 or later, running on Java 21");
    }

    @Test
    public void explainsMissingMultiReleaseClasses() {
        String message = MultiReleaseSupport.unavailableCause(25).getMessage();
        assertThat(message).contains("Running on Java 25");
        assertThat(message).contains("META-INF/versions/24");
        assertThat(message).contains("Multi-Release: true");
    }
}
