package no.entur.logging.cloud.logback.logstash.test.ai.junit;

import org.junit.jupiter.api.Test;

/** Fails like {@link Fixtures.SameCauseFixture}, in a file of its own. Run by {@link AiTestExecutionListenerTest} only. */
@Fixtures.Fixture
public class OtherFileFixture {

    @Test
    public void callsBrokenToo() {
        FixtureChecks.broken();
    }
}
