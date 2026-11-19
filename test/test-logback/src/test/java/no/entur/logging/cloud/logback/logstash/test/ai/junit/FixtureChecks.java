package no.entur.logging.cloud.logback.logstash.test.ai.junit;

/** A class which checks a value and logs a warning about it, for the caller frame in the summary (see {@link Fixtures.CallerFixture}). */
final class FixtureChecks {

    private FixtureChecks() {
    }

    static boolean isInRange(long value) {
        if (value < 0) {
            Fixtures.logger.warn("Value " + value + " is out of range");
            return false;
        }
        return true;
    }

    /** Fails, for tests in several files which fail the same way (see {@link Fixtures.SameCauseFixture}). */
    static void broken() {
        throw new IllegalStateException("broken");
    }
}
