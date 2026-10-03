package no.entur.logging.cloud.protobuf.json.codedoutputstream;

/**
 * The JSON-writing CodedOutputStream is in the Java 24+ part of the multi-release jar
 * (META-INF/versions/24). Explains why it is unavailable.
 */

final class MultiReleaseSupport {

    static final int REQUIRED_JAVA_VERSION = 24;

    private MultiReleaseSupport() {
    }

    /**
     * @param javaVersion the runtime's Java feature version
     * @return the reason why the Java 24+ classes are not in use
     */

    static UnsupportedOperationException unavailableCause(int javaVersion) {
        if (javaVersion < REQUIRED_JAVA_VERSION) {
            return new UnsupportedOperationException("Requires Java " + REQUIRED_JAVA_VERSION + " or later, running on Java " + javaVersion);
        }
        return new UnsupportedOperationException("Running on Java " + javaVersion + ", but the Java " + REQUIRED_JAVA_VERSION + "+ classes of the multi-release protobuf-json jar "
                + "(META-INF/versions/" + REQUIRED_JAVA_VERSION + ") were not loaded. If the jar was repackaged (i.e. shaded or merged into an uber jar), "
                + "the repackaged jar's manifest must contain 'Multi-Release: true'.");
    }
}
