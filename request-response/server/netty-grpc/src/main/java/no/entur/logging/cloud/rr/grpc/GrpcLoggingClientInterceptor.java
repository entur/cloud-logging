package no.entur.logging.cloud.rr.grpc;

/*-
 * #%L
 * abt-grpc-client-utils
 * %%
 * Copyright (C) 2019 - 2021 Entur
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.MessageOrBuilder;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import no.entur.logging.cloud.rr.grpc.filter.GrpcClientLoggingFilters;
import no.entur.logging.cloud.rr.grpc.filter.GrpcLogFilter;
import no.entur.logging.cloud.rr.grpc.filter.GrpcMetadataFilter;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcMetadataJsonMapper;
import no.entur.logging.cloud.rr.grpc.mapper.GrpcPayloadJsonMapper;
import no.entur.logging.cloud.rr.grpc.message.GrpcConnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcDisconnect;
import no.entur.logging.cloud.rr.grpc.message.GrpcRequest;
import no.entur.logging.cloud.rr.grpc.message.GrpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Interceptor which copies tracing headers from the current Context.
 */

public class GrpcLoggingClientInterceptor implements ClientInterceptor {

	private static final Logger log = LoggerFactory.getLogger(GrpcLoggingClientInterceptor.class);

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		private GrpcPayloadJsonMapper payloadJsonMapper;
		private GrpcMetadataJsonMapper metadataJsonMapper;
		private GrpcClientLoggingFilters filters;

		private GrpcSink sink;

		public Builder withFilters(GrpcClientLoggingFilters filters) {
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

		public GrpcLoggingClientInterceptor build() {
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

			return new GrpcLoggingClientInterceptor(sink, filters, metadataJsonMapper, payloadJsonMapper);
		}
	}

	protected final GrpcClientLoggingFilters filters;
	protected final GrpcMetadataJsonMapper metadataJsonMapper;

	protected final GrpcPayloadJsonMapper payloadJsonMapper;
	protected final GrpcSink sink;

	public GrpcLoggingClientInterceptor(GrpcSink sink, GrpcClientLoggingFilters filters, GrpcMetadataJsonMapper metadataJsonMapper, GrpcPayloadJsonMapper payloadJsonMapper) {
		this.sink = sink;
		this.filters = filters;
		this.metadataJsonMapper = metadataJsonMapper;
		this.payloadJsonMapper = payloadJsonMapper;
	}

	@Override
	public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
		if (!sink.isActive()) {
			return next.newCall(method, callOptions);
		}
		long timestamp = System.currentTimeMillis();

		String path = '/' + method.getFullMethodName();

		GrpcLogFilter filter = filters.getFilter(method.getServiceName(), method.getBareMethodName());

		if(!filter.isLogging()) {
			return next.newCall(method, callOptions);
		}

		AtomicInteger requestCounter = new AtomicInteger();

		ClientCall<ReqT, RespT> call = next.newCall(method, callOptions);

		String remoteAddress = getRemoteHostAddress(call);

		return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(call) {

			private Map<String, Object> requestHeaders = null;

			@Override
			public void start(Listener<RespT> listener, Metadata headers) {
				if (filter.isRequest() || filter.isConnect()) {
					requestHeaders = metadataJsonMapper.map(headers, filter.getRequestMetadataFilter());
				}

				GrpcConnect connectMessage;
				if (filter.isConnect()) {
					connectMessage = new GrpcConnect(requestHeaders, remoteAddress, path, "local");
					sink.connectMessage(connectMessage);
				} else {
					connectMessage = null;
				}

				Listener<RespT> responseListener;
				if (filter.isResponse() || filter.isDisconnect()) {
					responseListener = new Listener<RespT>() {

						// send headers and send message can be invoked multiple times per instance.
						private AtomicInteger responseCounter = new AtomicInteger();

						private Map<String, Object> responseHeaders = null;

						private AtomicLong messageSize = filter.isDisconnect() ? new AtomicLong(0) : null;

						@Override
						public void onHeaders(Metadata headers) {
							listener.onHeaders(headers);

							if (filter.isResponse()) {
								responseHeaders = metadataJsonMapper.map(headers, filter.getResponseMetadataFilter());
							}
						}

						@Override
						public void onMessage(RespT message) {
							if (filter.isResponse()) {
								MessageOrBuilder m = (MessageOrBuilder) message;

								int count = responseCounter.incrementAndGet();

								String body = null;
								try {
									body = payloadJsonMapper.map(m, filter.getResponseBodyFilter());
								} catch (Throwable e) {
									// came from others, log as info
									log.info("Cannot format protobuf response message", e);
								}

								GrpcResponse responseMessage = new GrpcResponse(responseHeaders, remoteAddress, path, body, "remote", count, Status.Code.OK);

								sink.responseMessage(responseMessage);
							} else if (filter.isDisconnect()) {
								// just increment counter
								responseCounter.incrementAndGet();
							}

							if (messageSize != null) {
								AbstractMessage m = (AbstractMessage) message;
								messageSize.addAndGet(m.getSerializedSize()); // TODO expensive?
							}

							listener.onMessage(message);
						}

						@Override
						public void onClose(Status status, Metadata trailers) {
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

									GrpcResponse responseMessage = new GrpcResponse(headers, remoteAddress, path, null, "remote", count, status.getCode());

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

									GrpcDisconnect disconnectMessage = new GrpcDisconnect(headers, remoteAddress, path,  "remote", requestCounter.get(), responseCounter.get(), messageSize.get(), duration);

									sink.disconnectMessage(connectMessage, disconnectMessage);
								}
							} finally {
								listener.onClose(status, trailers);
							}
						}

						@Override
						public void onReady() {
							listener.onReady();
						}
					};
				} else {
					responseListener = listener;
				}
				super.start(responseListener, headers);
			}

			@Override
			public void sendMessage(ReqT message) {
				if (filter.isRequest()) {
					MessageOrBuilder m = (MessageOrBuilder) message;

					int count = requestCounter.incrementAndGet();

					String body = null;
					try {
						body = payloadJsonMapper.map(m, filter.getRequestBodyFilter());
					} catch (Throwable e) {
						// came from us, so log as warn
						log.warn("Cannot format protobuf request message", e);
					}

					GrpcRequest requestMessage = new GrpcRequest(requestHeaders, null, path, body, "local", count);

					sink.requestMessage(requestMessage);
				} else if (filter.isDisconnect()) {
					// just increment counter
					requestCounter.incrementAndGet();
				}

				super.sendMessage(message);
			}
		};
	}

	private <ReqT, RespT> String getRemoteHostAddress(ClientCall<ReqT, RespT> call) {
		SocketAddress socketAddress = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
		if (socketAddress != null) {
			if (socketAddress instanceof InetSocketAddress) {
				InetSocketAddress isa = (InetSocketAddress) socketAddress;
				return isa.getAddress().getHostAddress();
			}
		}
		return null;
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
}
