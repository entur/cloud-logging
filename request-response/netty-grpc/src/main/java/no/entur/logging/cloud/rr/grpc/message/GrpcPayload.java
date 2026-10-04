package no.entur.logging.cloud.rr.grpc.message;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import no.entur.logging.cloud.rr.grpc.filter.GrpcBodyFilter;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcPayloadJsonMapper;
import tools.jackson.core.io.JsonStringEncoder;

/**
 * Message body, mapped to JSON at most once. Mapping can be deferred until the body is actually logged,
 * for example so that on-demand logging does not map messages for log statements which are discarded.
 * <br><br>
 * Deferred mapping holds on to the protobuf message, and might map it on another thread (i.e. the thread which
 * flushes an on-demand logging scope, or the async appender's worker thread). This is safe because:
 * <ul>
 *     <li>generated protobuf messages are immutable, "just like a Java String", see
 *     <a href="https://protobuf.dev/reference/java/java-generated/#message">Java Generated Code Guide: Messages</a> and
 *     <a href="https://protobuf.dev/getting-started/javatutorial/">Protocol Buffer Basics: Java</a> (Builders vs. Messages)</li>
 *     <li>gRPC's protobuf marshaller relies on this ("Returning provided object is safe since protobufs are immutable"), and
 *     received messages do not refer to the marshaller's reused read buffer (no aliasing), see
 *     <a href="https://github.com/grpc/grpc-java/blob/master/protobuf-lite/src/main/java/io/grpc/protobuf/lite/ProtoLiteUtils.java">ProtoLiteUtils</a></li>
 *     <li>the mapped body is published via volatile fields</li>
 * </ul>
 * Builders are not thread-safe (see the guide's <a href="https://protobuf.dev/reference/java/java-generated/#builders">Builders</a> section),
 * so a builder is snapshot (as an immutable message) when the payload is created; later changes to the builder are not reflected.
 * <br><br>
 * The reference to the protobuf message is released once the message has been mapped (also if mapping failed), so that
 * the (possibly large) message does not stay in memory for as long as the payload.
 */

public class GrpcPayload {

    /**
     * Create an already mapped payload.
     *
     * @param body body as raw JSON, or null
     * @return payload
     */

    public static GrpcPayload of(String body) {
        return new GrpcPayload(body);
    }

    protected volatile MessageOrBuilder message;
    protected final GrpcBodyFilter filter;
    protected final GrpcPayloadJsonMapper mapper;

    protected volatile boolean mapped;
    protected volatile String body;

    public GrpcPayload(MessageOrBuilder message, GrpcBodyFilter filter, GrpcPayloadJsonMapper mapper) {
        // builders are mutable and not thread-safe, so take an immutable snapshot
        this.message = message instanceof Message.Builder builder ? builder.buildPartial() : message;
        this.filter = filter;
        this.mapper = mapper;
    }

    protected GrpcPayload(String body) {
        this.message = null;
        this.filter = null;
        this.mapper = null;
        this.body = body;
        this.mapped = true;
    }

    /**
     * Map the message to JSON, if not already mapped.
     *
     * @throws InvalidProtocolBufferException if the message could not be mapped; the body is then null.
     */

    public void map() throws InvalidProtocolBufferException {
        if (!mapped) {
            synchronized (this) {
                if (!mapped) {
                    try {
                        body = mapper.map(message, filter);
                    } finally {
                        mapped = true;
                        message = null;
                    }
                }
            }
        }
    }

    /**
     * Get the body, mapping the message to JSON if not already mapped.
     * <br>
     * Mapping failures are not logged, as this method might be invoked by the logging framework itself.
     * Instead, the body describes the failure.
     *
     * @return body as raw JSON, or null
     */

    public String getBody() {
        if (!mapped) {
            synchronized (this) {
                if (!mapped) {
                    try {
                        body = mapper.map(message, filter);
                    } catch (Throwable e) {
                        body = getUnableToMapMessage(e);
                    } finally {
                        mapped = true;
                        message = null;
                    }
                }
            }
        }
        return body;
    }

    public boolean isMapped() {
        return mapped;
    }

    /**
     * Get the message
     *
     * @return the protobuf message, or null if it has been mapped (the reference is then released) or if created from an already mapped body.
     */

    public MessageOrBuilder getMessage() {
        return message;
    }

    protected String getUnableToMapMessage(Throwable e) {
        StringBuilder builder = new StringBuilder(128);
        builder.append('"');
        JsonStringEncoder.getInstance().quoteAsString("Unable to format message: " + e.getMessage(), builder);
        builder.append('"');
        return builder.toString();
    }
}
