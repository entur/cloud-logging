package no.entur.logging.cloud.protobuf.json.codedoutputstream;

import com.google.protobuf.CodedOutputStream;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonCodedOutputStreamFactoryTest {

    @Test
    public void requiresJava24() {
        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, () -> JsonCodedOutputStreamFactory.defineConstructor(21));
        assertThat(e).hasMessageThat().isEqualTo("Requires Java 24 or later, running on Java 21");
    }

    @Test
    public void definesConstructor() throws Throwable {
        // tests run on Java 25
        MethodHandle constructor = JsonCodedOutputStreamFactory.defineConstructor(Runtime.version().feature());
        assertThat(constructor.type().returnType()).isEqualTo(CodedOutputStream.class);

        assertThat(JsonCodedOutputStreamFactory.isAvailable()).isTrue();
        assertThat(JsonCodedOutputStreamFactory.getUnavailableCause()).isNull();
    }
}
