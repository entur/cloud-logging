package no.entur.logging.cloud.rr.grpc;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.MessageOrBuilder;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import no.entur.logging.cloud.rr.grpc.message.GrpcConnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcDisconnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcRequest;
import no.entur.logging.cloud.rr.grpc.message.GrpcResponse;
import no.entur.logging.cloud.rr.grpc.filter.GrpcLogFilter;
import no.entur.logging.cloud.rr.grpc.filter.GrpcMetadataFilter;
import no.entur.logging.cloud.rr.grpc.filter.GrpcServerLoggingFilters;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcPayloadJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcMetadataJsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Logbook-style request-response-logging.
 *
 */

public class GrpcLoggingServerInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GrpcLoggingServerInterceptor.class);

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private GrpcPayloadJsonMapper payloadJsonMapper;
        private GrpcMetadataJsonMapper metadataJsonMapper;
        private GrpcServerLoggingFilters filters;

        private GrpcSink sink;

        public Builder withFilters(GrpcServerLoggingFilters filters) {
            this.filters = filters;
            return this;
        }

        public Builder withSink(GrpcSink sink) {
            this.sink = sink;
            return this;
        }

        public Builder withMetadataJsonMapper(GrpcMetadataJsonMapper mapper) {
            this.metadataJsonMapper = mapper;
            return this;
        }

        public Builder withPayloadJsonMapper(GrpcPayloadJsonMapper payloadJsonMapper) {
            this.payloadJsonMapper = payloadJsonMapper;
            return this;
        }

        public GrpcLoggingServerInterceptor build() {
            if(payloadJsonMapper == null) {
                throw new IllegalStateException();
            }
            if(metadataJsonMapper == null) {
                throw new IllegalStateException();
            }
            if(filters == null) {
                throw new IllegalStateException();
            }
            if(sink == null) {
                throw new IllegalStateException();
            }

            return new GrpcLoggingServerInterceptor(sink, filters, metadataJsonMapper, payloadJsonMapper);
        }


    }

    protected final GrpcServerLoggingFilters filters;
    protected final GrpcMetadataJsonMapper metadataJsonMapper;

    protected final GrpcPayloadJsonMapper payloadJsonMapper;
    protected final GrpcSink sink;

    public GrpcLoggingServerInterceptor(GrpcSink sink, GrpcServerLoggingFilters filters, GrpcMetadataJsonMapper metadataJsonMapper, GrpcPayloadJsonMapper payloadJsonMapper) {
        this.sink = sink;
        this.filters = filters;
        this.metadataJsonMapper = metadataJsonMapper;
        this.payloadJsonMapper = payloadJsonMapper;
    }

    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, final Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        if (!sink.isActive()) {
            return next.startCall(call, headers);
        }
        long timestamp = System.currentTimeMillis();

        MethodDescriptor<ReqT, RespT> methodDescriptor = call.getMethodDescriptor();

        String path = '/' + methodDescriptor.getFullMethodName();

        GrpcLogFilter filter = filters.getFilter(methodDescriptor.getServiceName(), methodDescriptor.getBareMethodName());

        if(!filter.isLogging()) {
            return next.startCall(call, headers);
        }

        final Map<String, Object> requestHeaders = metadataJsonMapper.map(headers, filter.getRequestMetadataFilter());

        AtomicInteger requestCounter = new AtomicInteger();

        // XXX using experimental api
        String remoteAddress = getRemoteHostAddress(call);

        GrpcConnect connectMessage;
        if (filter.isConnect()) {
            connectMessage = new GrpcConnect(requestHeaders, remoteAddress, path, "remote");
            sink.connectMessage(connectMessage);
        } else {
            connectMessage = null;
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
                        responseHeaders = metadataJsonMapper.map(headers, filter.getResponseMetadataFilter());
                    }
                }

                @Override
                public void sendMessage(RespT message) {
                    if (filter.isResponse()) {
                        MessageOrBuilder m = (MessageOrBuilder) message;

                        int count = responseCounter.incrementAndGet();

                        String body = null;
                        try {
                            body = payloadJsonMapper.map(m, filter.getResponseBodyFilter());
                        } catch (Throwable e) {
                            // came from us, log as warn
                            log.warn("Cannot format protobuf response message", e);
                        }

                        GrpcResponse responseMessage = new GrpcResponse(responseHeaders, remoteAddress, path, body, "local", count, Status.Code.OK);

                        sink.responseMessage(responseMessage);
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
                            Map<String, Object> headers = toHeaders(status, trailers, filter.getResponseMetadataFilter());
                            int count = responseCounter.incrementAndGet();

                            GrpcResponse responseMessage = new GrpcResponse(headers, remoteAddress, path, null, "local", count, status.getCode());

                            sink.responseMessage(responseMessage);
                        }

                        if (filter.isDisconnect()) {
                            // if status not ok, and status not already logged in response, include status here
                            Map<String, Object> headers;
                            if (!status.isOk() && !filter.isResponse()) {
                                headers = toHeaders(status, trailers, filter.getResponseMetadataFilter());
                            } else {
                                headers = metadataJsonMapper.map(trailers, filter.getResponseMetadataFilter());
                            }
                            long duration = System.currentTimeMillis() - timestamp;

                            GrpcDisconnect disconnectMessage = new GrpcDisconnect(headers, remoteAddress, path,  "local", requestCounter.get(), responseCounter.get(), messageSize.get(), duration);

                            sink.disconnectMessage(connectMessage, disconnectMessage);
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

                        int count = requestCounter.incrementAndGet();

                        long timeRemainingUntilDeadlineInMilliseconds = getTimeRemainingUntilDeadlineInMilliseconds();

                        String body = null;
                        try {
                            body = payloadJsonMapper.map(m, filter.getRequestBodyFilter());
                        } catch (Throwable e) {
                            // came from someone else, so log as info
                            log.info("Cannot format protobuf request message", e);
                        }

                        GrpcRequest requestMessage = new GrpcRequest(requestHeaders, remoteAddress, path, body, "remote", count, timeRemainingUntilDeadlineInMilliseconds);

                        sink.requestMessage(requestMessage);
                    } else if (filter.isDisconnect()) {
                        // just increment counter
                        requestCounter.incrementAndGet();
                    }

                    super.onMessage(message);
                }

                private static long getTimeRemainingUntilDeadlineInMilliseconds() {
                    Context requestContext = Context.current();
                    if(requestContext != null) {
                        Deadline deadline = requestContext.getDeadline();
                        if (deadline != null) {
                            return deadline.timeRemaining(TimeUnit.MILLISECONDS);
                        }
                    }
                    return -1L;
                }
            };
        }
        return next.startCall(interceptCall, headers);
    }

    private Map<String, Object> toHeaders(Status status, Metadata trailers, GrpcMetadataFilter filter) {
        Map<String, Object> headers = metadataJsonMapper.map(trailers, filter); // note: can return null
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



}
