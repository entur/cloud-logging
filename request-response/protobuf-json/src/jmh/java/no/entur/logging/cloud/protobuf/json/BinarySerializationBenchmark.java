package no.entur.logging.cloud.protobuf.json;

import com.google.protobuf.Message;
import no.entur.logging.cloud.protobuf.json.codedoutputstream.CodedOutputStreamProtobufJsonWriter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.json.JsonFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Check whether using the JSON-writing CodedOutputStream slows down regular binary serialization in the same JVM,
 * i.e. by making call sites in the generated code megamorphic.
 */

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class BinarySerializationBenchmark {

    @Param({"false", "true"})
    public boolean jsonCodedOutputStreamUsed;

    @Param({"medium", "large"})
    public String payload;

    private Message message;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream(1 << 20);

    @Setup
    public void setup() throws IOException {
        message = BenchmarkPayloads.create(payload);

        // same binary serialization warmup in both variants, so that the only difference is the JSON writing
        ProtobufJsonWriter writer = CodedOutputStreamProtobufJsonWriter.newBuilder().withTypeRegistry(RandomMessages.createTypeRegistry()).build();
        JsonFactory factory = JsonFactory.builder().build();
        Message[] messages = {BenchmarkPayloads.create("small"), BenchmarkPayloads.create("medium"), BenchmarkPayloads.create("large")};
        for (int i = 0; i < 20_000; i++) {
            for (Message m : messages) {
                m.toByteArray();
                output.reset();
                m.writeTo(output);
                if (jsonCodedOutputStreamUsed) {
                    output.reset();
                    try (JsonGenerator generator = factory.createGenerator(output)) {
                        writer.write(m, generator);
                    }
                }
            }
        }
    }

    @Benchmark
    public int toByteArray() {
        return message.toByteArray().length;
    }

    @Benchmark
    public int writeToOutputStream() throws IOException {
        // like gRPC
        output.reset();
        message.writeTo(output);
        return output.size();
    }
}
