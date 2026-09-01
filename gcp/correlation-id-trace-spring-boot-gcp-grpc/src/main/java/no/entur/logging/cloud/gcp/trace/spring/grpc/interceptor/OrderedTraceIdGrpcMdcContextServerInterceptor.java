package no.entur.logging.cloud.gcp.trace.spring.grpc.interceptor;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import no.entur.logging.cloud.grpc.mdc.GrpcMdcContext;
import no.entur.logging.cloud.grpc.trace.CorrelationIdGrpcMdcContext;
import org.springframework.core.Ordered;

import java.util.concurrent.ThreadLocalRandom;

@Deprecated
public class OrderedTraceIdGrpcMdcContextServerInterceptor implements ServerInterceptor, Ordered {

    // The span ID is expected to be a 16-character, hexadecimal encoding of an 8-byte array and should not be zero. It should be unique within the trace and should, ideally, be generated in a manner that is uniformly random.
    public static final String SPAN_ID_MDC_KEY = "logging.googleapis.com/spanId";

    // Note: the bare trace id is the preferred format; the projects/[PROJECT-ID]/traces/[TRACE-ID]
    // resource name is a legacy format. Both are recognized by the Logs Explorer and the Trace Viewer
    // to group log entries and display them in line with traces.
    // See https://docs.cloud.google.com/trace/docs/trace-log-integration

    public static final String TRACE_MDC_KEY = "logging.googleapis.com/trace";

    private int order;

    public OrderedTraceIdGrpcMdcContextServerInterceptor(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        GrpcMdcContext grpcMdcContext = GrpcMdcContext.get();
        if(grpcMdcContext == null) {
            return next.startCall(call, headers);
        }

        // this interceptor runs after the CorrelationId interceptor, so there should always be a value here
        if(!grpcMdcContext.containsKey(TRACE_MDC_KEY)) {
            String correlationId = grpcMdcContext.get(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY);

            grpcMdcContext.put(TRACE_MDC_KEY, correlationId);
        }

        if(!grpcMdcContext.containsKey(SPAN_ID_MDC_KEY)) {
            //  16-character, hexadecimal encoding of an 8-byte array
            ThreadLocalRandom current = ThreadLocalRandom.current();
            long random = current.nextLong();
            String spanId = Long.toHexString(random);
            grpcMdcContext.put(SPAN_ID_MDC_KEY, spanId);
        }

        // no new context necessary
        return next.startCall(call, headers);
    }
}
