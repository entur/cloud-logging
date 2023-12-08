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

import io.grpc.*;
import no.entur.logging.cloud.grpc.trace.GrpcTraceMdcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Interceptor which adds correlation id
 */

public class AddNewCorrelationIdToRequestClientInterceptor implements ClientInterceptor {

	private static final Logger log = LoggerFactory.getLogger(AddNewCorrelationIdToRequestClientInterceptor.class);

	@Override
	public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
		String correlationId = UUID.randomUUID().toString();
		return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

			@Override
			public void start(Listener<RespT> responseListener, Metadata metadata) {
				metadata.put(GrpcTraceMdcContext.CORRELATION_ID_HEADER_KEY, correlationId);
				super.start(responseListener, metadata);
			}
		};
	}
}
