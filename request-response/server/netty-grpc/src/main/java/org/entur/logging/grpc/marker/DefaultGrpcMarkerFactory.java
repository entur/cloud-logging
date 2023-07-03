package org.entur.logging.grpc.marker;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.grpc.Metadata;
import io.grpc.protobuf.lite.ProtoLiteUtils;
import org.entur.logging.grpc.JsonPrinterFactory;
import org.entur.logging.grpc.TypeRegistryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultGrpcMarkerFactory implements GrpcMarkerFactory {

	// from StatusProto
	protected static final Metadata.Key<com.google.rpc.Status> STATUS_DETAILS_KEY =
			Metadata.Key.of(
					"grpc-status-details-bin",
					ProtoLiteUtils.metadataMarshaller(com.google.rpc.Status.getDefaultInstance()));


	private static final Logger log = LoggerFactory.getLogger(DefaultGrpcMarkerFactory.class);

	/**
	 * Field for indicating whether this intercepter is attached to a service or a client.
	 * The log level will be increased from info to warning if there is a formatting problem
	 * with messages produced by us.
	 */

	private final boolean client;

	/**
	 * We want to avoid deserialization of binary payloads to JSON, if that JSON is too large to be logged (100k
	 * limit in Stackdriver). The JSON size however are more related to class and field names than the binary
	 * output size, so keep track of the approximate limit per message type (i.e. class). <br/><br/>
	 * <p>
	 * In addition, keep a global limit based on rough, previously known, size estimates.
	 */

	private Map<Class<?>, Integer> previouslyTooLargeBinaryMessageLengths = new ConcurrentHashMap<>();
	
	private final JsonFormat.Printer printer;
	
	// Max length of logged JSON message
	private final int maxJsonMessageLength;

	// max size of binary payload for attempted deserialize (from binary to JSON)
	private int maxBinaryMessageLength;

	// https://nilsmagnus.github.io/post/proto-json-sizes/

	public DefaultGrpcMarkerFactory(JsonFormat.Printer printer, int maxJsonMessageLength) {
		this(printer, maxJsonMessageLength, (40 * maxJsonMessageLength) / 100);
	}

	public DefaultGrpcMarkerFactory(JsonFormat.Printer printer, int maxJsonMessageLength, int maxBinaryMessageLength) {
		this(printer, maxJsonMessageLength, maxBinaryMessageLength, false);
	}

	public DefaultGrpcMarkerFactory(JsonFormat.Printer printer, int maxJsonMessageLength, int maxBinaryMessageLength, boolean client) {
		this.printer = printer;
		this.maxJsonMessageLength = maxJsonMessageLength;
		this.maxBinaryMessageLength = maxBinaryMessageLength;

		this.client = client;
	}

	public DefaultGrpcMarkerFactory(boolean prettyPrint, int maxJsonMessageLength) {
		this(prettyPrint, maxJsonMessageLength, (40 * maxJsonMessageLength) / 100);
	}

	public DefaultGrpcMarkerFactory(boolean prettyPrint, int maxJsonMessageLength, int maxBinaryMessageLength) {
		this(prettyPrint, maxJsonMessageLength, maxBinaryMessageLength, false);
	}

	public DefaultGrpcMarkerFactory(boolean prettyPrint, int maxJsonMessageLength, int maxBinaryMessageLength, boolean client) {
		this.printer = JsonPrinterFactory.createPrinter(prettyPrint, TypeRegistryFactory.createDefaultTypeRegistry());
		this.maxJsonMessageLength = maxJsonMessageLength;
		this.maxBinaryMessageLength = maxBinaryMessageLength;

		this.client = client;
	}

	@Override
	public Marker createConnectMarker(Map<String, Object> requestHeaders, String remoteAddress, String path, String string) {
		return new GrpcConnectMarker(requestHeaders, remoteAddress, path, string);
	}

	@Override
	public Marker createResponseMarker(Map<String, Object> responseHeaders, String remoteAddress, String path, MessageOrBuilder m, String origin, int code) {
		String message = serialize(m, false);

		if (message != null) {
			if (message.length() <= 2) {
				// i.e. "{}"
				message = null;
			}
		} else {
			// formatting failed
			message = getUnableToFormatMessage();
		}

		return new GrpcResponseMarker(responseHeaders, remoteAddress, path, message, origin, code);
	}

	@Override
	public GrpcRequestMarker createRequestMarker(Map<String, Object> requestHeaders, String remoteAddress, String path, MessageOrBuilder m, String origin) {

		String message = serialize(m, true);

		String method;

		// XXX extract this value from underlying system, if possible
		if (message != null) {
			if (message.length() > 2) {
				method = "POST";
			} else {
				// i.e. "{}"
				method = "GET";
				message = null;
			}
		} else {
			// formatting failed
			method = "POST";
			message = getUnableToFormatMessage();
		}

		return new GrpcRequestMarker(requestHeaders, remoteAddress, path, message, method, origin);
	}

	protected String serialize(MessageOrBuilder m, boolean request) {
		String message;

		try {

			if (m instanceof AbstractMessage) {
				AbstractMessage am = (AbstractMessage) m;

				int serializedSize = am.getSerializedSize();

				Integer maxPreviouslyTooLargeBinaryMessageLength = previouslyTooLargeBinaryMessageLengths.get(m.getClass());
				if (serializedSize > maxBinaryMessageLength || (maxPreviouslyTooLargeBinaryMessageLength != null && serializedSize >= maxPreviouslyTooLargeBinaryMessageLength)) {
					// so it is too big, don't even try to serialize to JSON
					message = getTruncatedBinaryMessage(serializedSize);
				} else {
					// serialize to JSON, check if too big
					message = printer.print(m);

					if (message.length() > maxJsonMessageLength) {
						message = getTruncatedJsonMessage(message);

						// whoops, JSON was too large to log 
						// adjust max message length to later

						previouslyTooLargeBinaryMessageLengths.put(m.getClass(), serializedSize);
					}
				}
			} else if (m != null) {
				message = printer.print(m);

				if (message.length() > maxJsonMessageLength) {
					message = getTruncatedJsonMessage(message);
				}
			} else {
				message = "{}"; // i.e. empty. This kicks in when throwing StatusRuntimeExceptions
			}
		} catch (Throwable e) {
			// if this message was produced by us, log as warning.
			if (request) {
				if (client) {
					// came from us
					log.warn("Cannot format protobuf client request message", e);
				} else {
					// came from someone else
					log.info("Cannot format protobuf request message {}", e.toString());
				}
			} else {
				if (client) {
					// came from someone else
					log.info("Cannot format protobuf client response message {}", e.toString());
				} else {
					// came from us
					log.warn("Cannot format protobuf response message", e);
				}
			}

			message = null;
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

	@Override
	public Marker createGrpcDisconnectMarker(Map<String, Object> headers, String remoteAddress, String path, String origin) {
		return new GrpcDisconnectMarker(headers, remoteAddress, path, origin);
	}

}
