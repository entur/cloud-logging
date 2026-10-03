package no.entur.logging.cloud.protobuf.json;

import com.google.protobuf.Message;
import no.entur.logging.cloud.protobuf.json.codedoutputstream.CodedOutputStreamProtobufJsonWriter;
import no.entur.logging.cloud.protobuf.json.jsonformat.JsonFormatProtobufJsonWriter;
import no.entur.logging.cloud.protobuf.json.transcode.TranscodingProtobufJsonWriter;
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
 * Compare JsonFormat (as used for gRPC request-response logging today) with {@linkplain ProtobufJsonWriter}.
 * Each benchmark writes a message to a (byte-based) JSON generator, like logstash-logback-encoder does.
 */

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class ProtobufJsonWriterBenchmark {

    @Param({"small", "medium", "large"})
    public String payload;

    private Message message;

    private final JsonFactory factory = JsonFactory.builder().build();
    private final ByteArrayOutputStream output = new ByteArrayOutputStream(1 << 20);

    private final ProtobufJsonWriter jsonFormatWriter = JsonFormatProtobufJsonWriter.create(RandomMessages.createTypeRegistry());
    private final ProtobufJsonWriter codedOutputStreamWriter = CodedOutputStreamProtobufJsonWriter.newBuilder().withTypeRegistry(RandomMessages.createTypeRegistry()).build();
    private final ProtobufJsonWriter transcodingWriter = TranscodingProtobufJsonWriter.newBuilder().withTypeRegistry(RandomMessages.createTypeRegistry()).build();

    @Setup
    public void setup() {
        message = BenchmarkPayloads.create(payload);
        if (!CodedOutputStreamProtobufJsonWriter.isAvailable()) {
            throw new IllegalStateException("CodedOutputStream not available", CodedOutputStreamProtobufJsonWriter.getUnavailableCause());
        }
    }

    @Benchmark
    public int jsonFormat() throws IOException {
        output.reset();
        try (JsonGenerator generator = factory.createGenerator(output)) {
            jsonFormatWriter.write(message, generator);
        }
        return output.size();
    }

    @Benchmark
    public int codedOutputStream() throws IOException {
        output.reset();
        try (JsonGenerator generator = factory.createGenerator(output)) {
            codedOutputStreamWriter.write(message, generator);
        }
        return output.size();
    }

    @Benchmark
    public int transcoding() throws IOException {
        output.reset();
        try (JsonGenerator generator = factory.createGenerator(output)) {
            transcodingWriter.write(message, generator);
        }
        return output.size();
    }

    @Benchmark
    public int binary() {
        // reference
        return message.toByteArray().length;
    }
}
