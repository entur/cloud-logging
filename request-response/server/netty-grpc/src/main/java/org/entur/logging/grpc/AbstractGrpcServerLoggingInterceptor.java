package org.entur.logging.grpc;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.MessageOrBuilder;
import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.entur.logging.grpc.filter.GrpcLogFilter;
import org.entur.logging.grpc.filter.GrpcServerLoggingFilters;
import org.entur.logging.grpc.marker.GrpcMarkerFactory;
import org.entur.logging.grpc.marker.GrpcRequestMarker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractGrpcServerLoggingInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AbstractGrpcServerLoggingInterceptor.class);

    protected final GrpcMarkerFactory grpcMarkerFactory;
    protected final GrpcServerLoggingFilters filters;
    protected final MetadataJsonMapper mapper;

    public AbstractGrpcServerLoggingInterceptor(GrpcMarkerFactory grpcMarkerFactory, GrpcServerLoggingFilters filters, MetadataJsonMapper mapper) {
        this.grpcMarkerFactory = grpcMarkerFactory;
        this.filters = filters;
        this.mapper = mapper;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, final Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        long timestamp = System.currentTimeMillis();

        String path = "/" + call.getMethodDescriptor().getFullMethodName();

        GrpcLogFilter filter = filters.getFilter(path);

        if (log.isDebugEnabled() && filter.isLogging()) {
            final Map<String, Object> requestHeaders = mapper.toJson(headers);

            AtomicInteger requestCounter = new AtomicInteger();

            // XXX using experimental api
            String remoteAddress = getRemoteHostAddress(call);

            if (filter.isConnect()) {
                preLogStatement();
                try {
                    Marker marker = grpcMarkerFactory.createConnectMarker(requestHeaders, remoteAddress, path, "remote");

                    log.debug(marker, "Connect " + path);
                } finally {
                    postLogStatement();
                }
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

                            try {
                                preLogStatement();

                                Marker marker = grpcMarkerFactory.createResponseMarker(responseHeaders, remoteAddress, path, m, "local", 200);

                                int count = responseCounter.incrementAndGet();

                                // antar at alle meldinger som blir sendt kommuniseres tilbake med 200 OK
                                log.debug(marker, "Response #" + count + " 200 OK " + path);
                            } finally {
                                postLogStatement();
                            }
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
                            try {
                                preLogStatement();
                                // add a response on error state, as then the above sendMessage(..) is never invoked.
                                if (!status.isOk() && filter.isResponse()) {
                                    Map<String, Object> headers = toHeaders(status, trailers);
                                    Marker marker = grpcMarkerFactory.createResponseMarker(headers, remoteAddress, path, null, "local", 200);

                                    int count = responseCounter.incrementAndGet();

                                    // disconnecting and responding to input at the same time
                                    log.debug(marker, "Response #" + count + " " + status.getCode().value() + " " + status.getCode().name() + " " + path);
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
                                    log.debug(marker, verb + " " + path + " " + requests + " request" + (requests > 1 ? "s" : "") + " and " + responses + " response" + (responses > 1 ? "s" : "") + " (" + formatBytes(messageSize.get()) + ") in " + (System.currentTimeMillis() - timestamp) + "ms");
                                }
                            } finally {
                                postLogStatement();
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
                            preLogStatement();
                            try {
                                MessageOrBuilder m = (MessageOrBuilder) message;

                                GrpcRequestMarker marker = grpcMarkerFactory.createRequestMarker(requestHeaders, remoteAddress, path, m, "remote");

                                int count = requestCounter.incrementAndGet();

                                log.debug(marker, "Request #" + count + " " + marker.getMethod() + " " + path);
                            } finally {
                                postLogStatement();
                            }
                        } else if (filter.isDisconnect()) {
                            // just increment counter
                            requestCounter.incrementAndGet();
                        }

                        super.onMessage(message);
                    }
                };
            }
            return next.startCall(interceptCall, headers);
        } else {
            return next.startCall(call, headers);
        }
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

    public abstract void postLogStatement();

    public abstract void preLogStatement();

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
