package org.entur.example.web.otel.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;

import static com.google.common.truth.Truth.assertThat;

/**
 * Verifies that {@code TraceSampledMdcHandler} is NOT registered as a bean when the
 * OpenTelemetry Java agent is active. The agent provides its own MDC keys and the
 * autoconfiguration must not interfere.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class TraceSampledMdcHandlerAbsenceTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void traceSampledMdcHandlerBeanIsNotRegisteredWhenOtelAgentIsPresent() {
        assertThat(applicationContext.containsBean("traceSampledMdcHandler")).isFalse();
    }
}
