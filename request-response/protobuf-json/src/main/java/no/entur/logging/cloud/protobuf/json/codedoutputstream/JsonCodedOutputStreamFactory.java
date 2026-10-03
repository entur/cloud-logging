package no.entur.logging.cloud.protobuf.json.codedoutputstream;

import com.google.protobuf.CodedOutputStream;

/**
 * Factory for {@linkplain CodedOutputStream}s which forward all writes to a {@linkplain CodedOutputStreamSink}.
 * <br><br>
 * This version is unavailable; the Java 24+ version (in the multi-release jar) generates a subclass of
 * {@linkplain CodedOutputStream} using the ClassFile API. This version is also loaded on Java 24+ if the jar
 * was repackaged without the 'Multi-Release: true' manifest attribute.
 */

final class JsonCodedOutputStreamFactory {

    private static final Throwable UNAVAILABLE_CAUSE = MultiReleaseSupport.unavailableCause(Runtime.version().feature());

    private JsonCodedOutputStreamFactory() {
    }

    static boolean isAvailable() {
        return false;
    }

    static Throwable getUnavailableCause() {
        return UNAVAILABLE_CAUSE;
    }

    static CodedOutputStream create(CodedOutputStreamSink sink) {
        throw new IllegalStateException("Not available", UNAVAILABLE_CAUSE);
    }
}
