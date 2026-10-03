package no.entur.logging.cloud.protobuf.json.codedoutputstream;

import com.google.protobuf.CodedOutputStream;

/**
 * Factory for {@linkplain CodedOutputStream}s which forward all writes to a {@linkplain CodedOutputStreamSink}.
 * <br><br>
 * This version is unavailable; the Java 24+ version (in the multi-release jar) generates a subclass of
 * {@linkplain CodedOutputStream} using the ClassFile API.
 */

final class JsonCodedOutputStreamFactory {

    private static final Throwable UNAVAILABLE_CAUSE = new UnsupportedOperationException("Requires Java 24 or later, running on Java " + Runtime.version().feature());

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
