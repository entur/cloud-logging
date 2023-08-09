package no.entur.logging.cloud.rr.grpc.mapper;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import no.entur.logging.cloud.rr.grpc.filter.GrpcBodyFilter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultGrpcPayloadJsonMapper implements GrpcPayloadJsonMapper {

    private final JsonFormat.Printer printer;
    private Map<Class<?>, Integer> previouslyTooLargeBinaryMessageLengths = new ConcurrentHashMap<>();

    // Max length of logged JSON message
    private final int maxJsonMessageLength;

    // max size of binary payload for attempted deserialize (from binary to JSON)
    private final int maxBinaryMessageLength;

    public DefaultGrpcPayloadJsonMapper(JsonFormat.Printer printer, int maxJsonMessageLength, int maxBinaryMessageLength) {
        this.printer = printer;
        this.maxJsonMessageLength = maxJsonMessageLength;
        this.maxBinaryMessageLength = maxBinaryMessageLength;
    }

    @Override
    public String map(MessageOrBuilder m, GrpcBodyFilter filter) throws InvalidProtocolBufferException {
        String message = serialize(m, filter);

        if (message != null) {
            if (message.length() <= 2) {
                // i.e. "{}"
                message = null;
            }
        } else {
            // formatting failed
            message = getUnableToFormatMessage();
        }
        return message;
    }

    private String serialize(MessageOrBuilder m, GrpcBodyFilter filter) throws InvalidProtocolBufferException {

        String message = null;
        if (m instanceof AbstractMessage) {
            AbstractMessage am = (AbstractMessage) m;

            int serializedSize = am.getSerializedSize();

            Integer maxPreviouslyTooLargeBinaryMessageLength = previouslyTooLargeBinaryMessageLengths.get(m.getClass());
            if (serializedSize > maxBinaryMessageLength || (maxPreviouslyTooLargeBinaryMessageLength != null && serializedSize >= maxPreviouslyTooLargeBinaryMessageLength)) {
                // so it is too big, don't even try to serialize to JSON
                message = getTruncatedBinaryMessage(serializedSize);
            } else {
                // serialize to JSON, check if too big
                message = filter.filterBody(printer.print(m));

                if (message.length() > maxJsonMessageLength) {
                    message = getTruncatedJsonMessage(message);

                    // whoops, JSON was too large to log
                    // adjust max message length to later

                    previouslyTooLargeBinaryMessageLengths.put(m.getClass(), serializedSize);
                }
            }
        } else if (m != null) {
            message = filter.filterBody(printer.print(m));

            if (message.length() > maxJsonMessageLength) {
                message = getTruncatedJsonMessage(message);
            }
        } else {
            message = "{}"; // i.e. empty. This kicks in when throwing StatusRuntimeExceptions
        }
        return message;
    }

    /**
     * Truncate message
     *
     * @param message input message
     * @return truncated message; as raw JSON
     */

    protected String getTruncatedJsonMessage(String message) {
        return "\"Omitted JSON message size " + message.length() + "\"";
    }

    protected String getUnableToFormatMessage() {
        return "\"Unable to format message\"";
    }

    /**
     * Truncate message
     *
     * @param size size of the discarded message
     * @return truncated message; as raw JSON
     */

    protected String getTruncatedBinaryMessage(int size) {
        return "\"Omitted binary message size " + size + "\"";
    }

}
