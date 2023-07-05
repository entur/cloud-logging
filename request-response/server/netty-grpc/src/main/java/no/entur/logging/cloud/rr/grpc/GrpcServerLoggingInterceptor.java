package no.entur.logging.cloud.rr.grpc;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import no.entur.logging.cloud.rr.grpc.filter.GrpcLogFilter;
import no.entur.logging.cloud.rr.grpc.filter.GrpcServerLoggingFilters;
import no.entur.logging.cloud.rr.grpc.marker.DefaultGrpcMarkerFactory;
import no.entur.logging.cloud.rr.grpc.marker.GrpcMarkerFactory;
import no.entur.logging.cloud.rr.grpc.marker.GrpcRequestMarker;
import no.entur.logging.cloud.rr.grpc.status.GrpcStatusMapper;
import no.entur.logging.cloud.rr.grpc.status.JsonPrinterStatusMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static org.slf4j.event.EventConstants.DEBUG_INT;
import static org.slf4j.event.EventConstants.ERROR_INT;
import static org.slf4j.event.EventConstants.INFO_INT;
import static org.slf4j.event.EventConstants.TRACE_INT;
import static org.slf4j.event.EventConstants.WARN_INT;

/**
 *
 * Logbook-style request-response-logging.
 *
 */

public class GrpcServerLoggingInterceptor implements ServerInterceptor {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        public static final int DEFAULT_JSON_MESSAGE_SIZE = 99 * 1024;
        public static final int DEFAULT_BINARY_MESSAGE_SIZE = 40 * 1024;

        private boolean prettyPrint = false;
        private int maxJsonMessageLength = -1;
        private int maxBinaryMessageLength = -1;
        private GrpcServerLoggingFilters filters = null;
        private Map<String, Function<Metadata, Object>> keyMappers = new HashMap<>();

        private Logger logger;

        private Level level;

        private JsonFormat.TypeRegistry typeRegistry;

        public Builder withPrettyPrint(boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
            return this;
        }

        public Builder withLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder withLogLevel(Level level) {
            this.level = level;
            return this;
        }

        public Builder withKeyMapper(String key, Function<Metadata, Object> mapper) {
            this.keyMappers.put(key, mapper);
            return this;
        }

        public Builder withKeyMappers(Map<String, Function<Metadata, Object>> keyMappers) {
            this.keyMappers = keyMappers;
            return this;
        }

        public Builder withTypeRegistry(JsonFormat.TypeRegistry typeRegistry) {
            this.typeRegistry = typeRegistry;
            return this;
        }

        public Builder withMaxJsonMessageLength(int maxJsonMessageLength) {
            this.maxJsonMessageLength = maxJsonMessageLength;
            return this;
        }

        public Builder withMaxBinaryMessageLength(int maxBinaryMessageLength) {
            this.maxBinaryMessageLength = maxBinaryMessageLength;
            return this;
        }

        public Builder withFilters(GrpcServerLoggingFilters filters) {
            this.filters = filters;
            return this;
        }

        public GrpcServerLoggingInterceptor build() {
            if (maxJsonMessageLength == -1) {
                maxJsonMessageLength = DEFAULT_JSON_MESSAGE_SIZE;
            }

            if (maxBinaryMessageLength == -1) {
                maxBinaryMessageLength = DEFAULT_BINARY_MESSAGE_SIZE;
            }

            if (typeRegistry == null) {
                typeRegistry = TypeRegistryFactory.createDefaultTypeRegistry();
            }

            if(logger == null) {
                logger = LoggerFactory.getLogger(GrpcServerLoggingInterceptor.class);
            }

            JsonFormat.Printer printer = JsonPrinterFactory.createPrinter(prettyPrint, typeRegistry);

            DefaultGrpcMarkerFactory factory = new DefaultGrpcMarkerFactory(printer, maxJsonMessageLength, maxBinaryMessageLength);

            if (filters == null) {
                filters = GrpcServerLoggingFilters.classic();
            }
            if(level == null) {
                level = Level.DEBUG;
            }

            GrpcStatusMapper grpcStatusMapper = new JsonPrinterStatusMapper(printer);

            return new GrpcServerLoggingInterceptor(factory, filters, new DefaultMetadataJsonMapper(grpcStatusMapper, keyMappers), loggerToBiConsumer(), logEnabledToBooleanSupplier());
        }

        private BooleanSupplier logEnabledToBooleanSupplier() {
            int levelInt = level.toInt();
            switch (levelInt) {
                case (TRACE_INT):
                    return logger::isTraceEnabled;
                case (DEBUG_INT):
                    return logger::isDebugEnabled;
                case (INFO_INT):
                    return logger::isInfoEnabled;
                case (WARN_INT):
                    return logger::isWarnEnabled;
                case (ERROR_INT):
                    return logger::isErrorEnabled;
                default:
                    throw new IllegalStateException("Level [" + level + "] not recognized.");
            }
        }

        private BiConsumer<Marker, String> loggerToBiConsumer() {

            int levelInt = level.toInt();
            switch (levelInt) {
                case (TRACE_INT):
                    return logger::trace;
                case (DEBUG_INT):
                    return  logger::debug;
                case (INFO_INT):
                    return logger::info;
                case (WARN_INT):
                    return  logger::warn;
                case (ERROR_INT):
                    return logger::error;
                default:
                    throw new IllegalStateException("Level [" + level + "] not recognized.");
            }

        }

    }

    protected final GrpcMarkerFactory grpcMarkerFactory;
    protected final GrpcServerLoggingFilters filters;
    protected final MetadataJsonMapper mapper;

    protected final BiConsumer<Marker, String> logConsumer;
    protected final BooleanSupplier logLevelEnabled;

    public GrpcServerLoggingInterceptor(GrpcMarkerFactory grpcMarkerFactory, GrpcServerLoggingFilters filters, MetadataJsonMapper mapper, BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled) {
        this.grpcMarkerFactory = grpcMarkerFactory;
        this.filters = filters;
        this.mapper = mapper;
        this.logConsumer = logConsumer;
        this.logLevelEnabled = logLevelEnabled;
    }

    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, final Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        if (!logLevelEnabled.getAsBoolean()) {
            return next.startCall(call, headers);
        }
        long timestamp = System.currentTimeMillis();

        String path = '/' + call.getMethodDescriptor().getFullMethodName();

        GrpcLogFilter filter = filters.getFilter(path);

        if(!filter.isLogging()) {
            return next.startCall(call, headers);
        }

        final Map<String, Object> requestHeaders = mapper.toJson(headers);

        AtomicInteger requestCounter = new AtomicInteger();

        // XXX using experimental api
        String remoteAddress = getRemoteHostAddress(call);

        if (filter.isConnect()) {
            Marker marker = grpcMarkerFactory.createConnectMarker(requestHeaders, remoteAddress, path, "remote");

            logConsumer.accept(marker, "Connect " + path);
        }

        ServerCall<ReqT, RespT> interceptCall;
        if (filter.isResponse() || filter.isDisconnect()) {
            interceptCall = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {

                // send headers and send message can be invoked multiple times per instance.
                private AtomicInteger responseCounter = new AtomicInteger();

                private Map<String, Object> responseHeaders = null;

                private AtomicLong messageSize = filter.isDisconnect() ? new AtomicLong(0) : null;

                /**
                 * Send response header metadata prior to sending a response message. This method may
                 * only be called once and cannot be called after calls to {@link #sendMessage} or {@link #close}.
                 *
                 * <p>Since {@link Metadata} is not thread-safe, the caller must not access (read or write) {@code
                 * headers} after this point.
                 *
                 * @param headers metadata to send prior to any response body.
                 * @throws IllegalStateException if {@code close} has been called, a message has been sent, or headers have already been sent
                 */

                @Override
                public void sendHeaders(Metadata headers) {
                    // ser ut på ServerCallStreamObserverImpl#onNext at det er ett kall til sendHeaders og n kall til sendMessage,
                    // så dette burde være trygt.
                    super.sendHeaders(headers);

                    if (filter.isResponse()) {
                        responseHeaders = mapper.toJson(headers);
                    }
                }

                @Override
                public void sendMessage(RespT message) {
                    if (filter.isResponse()) {
                        MessageOrBuilder m = (MessageOrBuilder) message;

                        Marker marker = grpcMarkerFactory.createResponseMarker(responseHeaders, remoteAddress, path, m, "local", 200);

                        int count = responseCounter.incrementAndGet();

                        // antar at alle meldinger som blir sendt kommuniseres tilbake med 200 OK
                        logConsumer.accept(marker, "Response #" + count + " 200 OK " + path);
                    } else if (filter.isDisconnect()) {
                        // just increment counter
                        responseCounter.incrementAndGet();
                    }

                    if (messageSize != null) {
                        AbstractMessage m = (AbstractMessage) message;
                        messageSize.addAndGet(m.getSerializedSize()); // TODO expensive?
                    }

                    super.sendMessage(message);
                }

                @Override
                public void close(Status status, Metadata trailers) {
                    try {
                        if ((status.isOk() || !filter.isResponse()) && !filter.isDisconnect()) {
                            // do nothing
                            return;
                        }

                        // strictly speaking this is a response message to disconnect.

                        // add a response on error state, as then the above sendMessage(..) is never invoked.
                        if (!status.isOk() && filter.isResponse()) {
                            Map<String, Object> headers = toHeaders(status, trailers);
                            Marker marker = grpcMarkerFactory.createResponseMarker(headers, remoteAddress, path, null, "local", 200);

                            int count = responseCounter.incrementAndGet();

                            // disconnecting and responding to input at the same time
                            logConsumer.accept(marker, "Response #" + count + " " + status.getCode().value() + " " + status.getCode().name() + " " + path);
                        }

                        if (filter.isDisconnect()) {
                            // if status not ok, and status not already logged in response, include status here
                            Map<String, Object> headers;
                            if (!status.isOk() && !filter.isResponse()) {
                                headers = toHeaders(status, trailers);
                            } else {
                                headers = mapper.toJson(trailers);
                            }
                            Marker marker = grpcMarkerFactory.createGrpcDisconnectMarker(headers, remoteAddress, path, "local");

                            // just disconnecting
                            int requests = requestCounter.get();
                            int responses = responseCounter.get();
                            String verb;
                            if (!filter.isConnect()) {
                                verb = "Summary";
                            } else {
                                verb = "Disconnect";
                            }
                            logConsumer.accept(marker, verb + " " + path + " " + requests + " request" + (requests > 1 ? "s" : "") + " and " + responses + " response" + (responses > 1 ? "s" : "") + " (" + formatBytes(messageSize.get()) + ") in " + (System.currentTimeMillis() - timestamp) + "ms");
                        }

                    } finally {
                        super.close(status, trailers);
                    }

                }
            };
        } else {
            interceptCall = call;
        }

        if (filter.isRequest() || filter.isDisconnect()) {
            ServerCall.Listener<ReqT> listener = next.startCall(interceptCall, headers);
            return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
                @Override
                public void onMessage(ReqT message) {
                    if (filter.isRequest()) {
                        MessageOrBuilder m = (MessageOrBuilder) message;

                        GrpcRequestMarker marker = grpcMarkerFactory.createRequestMarker(requestHeaders, remoteAddress, path, m, "remote");

                        int count = requestCounter.incrementAndGet();

                        logConsumer.accept(marker, "Request #" + count + " " + marker.getMethod() + " " + path);
                    } else if (filter.isDisconnect()) {
                        // just increment counter
                        requestCounter.incrementAndGet();
                    }

                    super.onMessage(message);
                }
            };
        }
        return next.startCall(interceptCall, headers);
    }

    private Map<String, Object> toHeaders(Status status, Metadata trailers) {
        Map<String, Object> headers = mapper.toJson(trailers); // note: can return null
        if (!status.isOk()) {
            if (headers == null) {
                headers = new HashMap<>();
            }
            // These headers are not present yet, so add them here.
            // They are added by the root call in onClose(..).
            // However we prefer not to call onClose(..) because that would run
            // code in an above interceptor.
            headers.put("grpc-status", status.getCode().value()); // see InternalStatus.CODE_KEY
            if (status.getDescription() != null) {
                headers.put("grpc-message", status.getDescription()); // see InternalStatus.MESSAGE_KEY
            }
        }
        return headers;
    }

    private <ReqT, RespT> String getRemoteHostAddress(ServerCall<ReqT, RespT> call) {
        SocketAddress socketAddress = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        if (socketAddress != null) {
            if (socketAddress instanceof InetSocketAddress) {
                InetSocketAddress isa = (InetSocketAddress) socketAddress;
                return isa.getAddress().getHostAddress();
            }
        }
        return null;
    }

    private String formatBytes(long size) {
        if (size < 1024) {
            return size + " bytes";
        }
        if (size < 1024 * 1024) {
            return (size / 1024) + "KB";
        }
        return (size / (1024 * 1024)) + "MB";
    }


}
