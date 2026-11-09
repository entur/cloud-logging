package no.entur.logging.cloud.rr.grpc.mapper;

import com.google.protobuf.MessageOrBuilder;
import com.google.rpc.Status;
import no.entur.protobuf.json.ProtobufJsonWriter;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.util.RawValue;

import java.io.IOException;

/**
 * Map grpc status using protobuf-to-json, with a writer whose type registry supports the relevant Any types.
 */

public class ProtobufJsonWriterStatusMapper implements GrpcStatusMapper {

    private final ProtobufJsonWriter<MessageOrBuilder, JsonGenerator> writer;

    public ProtobufJsonWriterStatusMapper(ProtobufJsonWriter<MessageOrBuilder, JsonGenerator> writer) {
        this.writer = writer;
    }

    /**
     * Map grpc status to JSON.
     *
     * @param status input status
     * @return the status as {@linkplain RawValue}, or an error message (String) if the status could not be written.
     */

    @Override
    public Object map(Status status) {
        try {
            return new RawValue(writer.writeAsString(status));
        } catch (IOException e) {
            return "[logging interceptor could not print status: " + e.getMessage() + "]";
        }
    }
}
