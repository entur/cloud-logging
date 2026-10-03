package no.entur.logging.cloud.protobuf.json.codedoutputstream;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import no.entur.logging.cloud.protobuf.json.ProtobufJsonWriter;
import no.entur.logging.cloud.protobuf.json.plan.MessagePlan;
import no.entur.logging.cloud.protobuf.json.plan.MessagePlan.Plans;
import no.entur.logging.cloud.protobuf.json.transcode.TranscodingProtobufJsonWriter;
import tools.jackson.core.JsonGenerator;

import java.io.IOException;

/**
 * {@linkplain ProtobufJsonWriter} which lets a message's generated serialization code write JSON directly, via a
 * {@linkplain com.google.protobuf.CodedOutputStream} which produces JSON instead of the binary format.
 * This is the fastest implementation, and allocates the least.
 * <br><br>
 * CodedOutputStream only has a private constructor, so a subclass is generated at runtime (requires Java 24+).
 * This depends on how protobuf's generated code and CodedOutputStream work internally.
 * <br><br>
 * Falls back to {@linkplain TranscodingProtobufJsonWriter} for messages which are not generated (i.e.
 * {@linkplain com.google.protobuf.DynamicMessage}), and if not available (see {@linkplain #isAvailable()}).
 * The output is the same; see {@linkplain TranscodingProtobufJsonWriter} for differences from JsonFormat.
 */

public class CodedOutputStreamProtobufJsonWriter implements ProtobufJsonWriter {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private JsonFormat.TypeRegistry typeRegistry = JsonFormat.TypeRegistry.getEmptyTypeRegistry();

        /**
         * @param typeRegistry types for resolving google.protobuf.Any
         * @return this builder
         */

        public Builder withTypeRegistry(JsonFormat.TypeRegistry typeRegistry) {
            this.typeRegistry = typeRegistry;
            return this;
        }

        public CodedOutputStreamProtobufJsonWriter build() {
            return new CodedOutputStreamProtobufJsonWriter(typeRegistry);
        }
    }

    /**
     * @return whether a JSON-writing CodedOutputStream can be used in this runtime
     */

    public static boolean isAvailable() {
        return JsonCodedOutputStreamFactory.isAvailable();
    }

    /**
     * @return the reason why a JSON-writing CodedOutputStream cannot be used, or null if available
     */

    public static Throwable getUnavailableCause() {
        return JsonCodedOutputStreamFactory.getUnavailableCause();
    }

    private final Plans plans = new Plans();
    private final TranscodingProtobufJsonWriter transcoder;
    private final boolean available = isAvailable();

    protected CodedOutputStreamProtobufJsonWriter(JsonFormat.TypeRegistry typeRegistry) {
        this.transcoder = new TranscodingProtobufJsonWriter(plans, typeRegistry);
    }

    @Override
    public void write(MessageOrBuilder message, JsonGenerator generator) throws IOException {
        Message m;
        if (message instanceof Message msg) {
            m = msg;
        } else {
            m = ((Message.Builder) message).buildPartial();
        }

        MessagePlan plan = plans.get(m.getDescriptorForType());
        if (plan.wellKnownType != null) {
            transcoder.writeWellKnown(plan.wellKnownType, m, generator);
        } else if (available && m instanceof GeneratedMessage) {
            new CodedOutputStreamJsonSink(plans, transcoder, generator).write(m);
        } else {
            transcoder.write(m, generator);
        }
    }
}
