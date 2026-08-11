package org.entur.example.web.otel.starter;

import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import static org.assertj.core.api.Assertions.assertThat;

class TelemetryTest {

    @RegisterExtension
    static final OpenTelemetryExtension otelTesting = OpenTelemetryExtension.create();

    @Test
    void verifyCustomSpanLogsCorrectly() {
        // Run your business logic here

        var spans = otelTesting.getSpans();
        //assertThat(spans).isNotEmpty();
    }
}