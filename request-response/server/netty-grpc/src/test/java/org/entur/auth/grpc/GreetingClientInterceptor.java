package org.entur.auth.grpc;

import io.grpc.*;
import org.entur.oidc.grpc.test.GreetingMetadata;

public class GreetingClientInterceptor implements ClientInterceptor {

    public static final Metadata.Key<byte[]> SIGNED_TOKEN_METADATA_KEY = Metadata.Key.of("greeting-bin", Metadata.BINARY_BYTE_MARSHALLER);

    private GreetingMetadata greetingMetadata;

    public GreetingClientInterceptor(GreetingMetadata greetingMetadata) {
        this.greetingMetadata = greetingMetadata;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        ClientCall<ReqT, RespT> call = next.newCall(method, callOptions);

        call = new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(call) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {

                if (greetingMetadata != null) {
                    headers.put(SIGNED_TOKEN_METADATA_KEY, greetingMetadata.toByteArray());
                }

                super.start(responseListener, headers);
            }

        };
        return call;
    }

}
