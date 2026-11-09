package no.entur.logging.cloud.rr.grpc.mapper;

import com.google.protobuf.MessageOrBuilder;
import no.entur.logging.cloud.rr.grpc.filter.GrpcPayloadFilter;
import no.entur.protobuf.json.ProtobufJsonWriter;
import tools.jackson.core.JsonGenerator;

/**
 * Creates protobuf-to-json writers, one per payload filter.
 * <br><br>
 * The default is {@linkplain TranscodingProtobufJsonWriterFactory}. On Java 24+, the faster accessor writer can be used instead, i.e.
 *
 * <pre>
 * filter -&gt; filter.configure(AccessorProtobufJsonWriter.newBuilder().withTypeRegistry(typeRegistry).withMaxSize(maxSize)).build()
 * </pre>
 */

@FunctionalInterface
public interface ProtobufJsonWriterFactory {

    /**
     * Create a writer.
     *
     * @param filter payload filter
     * @return writer which applies the payload filter
     */

    ProtobufJsonWriter<MessageOrBuilder, JsonGenerator> create(GrpcPayloadFilter filter);

}
