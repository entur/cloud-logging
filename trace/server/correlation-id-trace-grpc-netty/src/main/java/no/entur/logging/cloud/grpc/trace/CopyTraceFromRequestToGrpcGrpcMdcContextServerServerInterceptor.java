package no.entur.logging.cloud.grpc.trace;

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import no.entur.logging.cloud.grpc.mdc.AbstractEnrichGrpcMdcContextServerInterceptor;
import no.entur.logging.cloud.grpc.mdc.EnrichGrpcMdcContextServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 *
 * Also adds request id
 *
 */

public class CopyTraceFromRequestToGrpcGrpcMdcContextServerServerInterceptor extends AbstractEnrichGrpcMdcContextServerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(CopyTraceFromRequestToGrpcGrpcMdcContextServerServerInterceptor.class);

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		private EnrichGrpcMdcContextServerInterceptor enricher;
		private boolean required = true;

		public CopyTraceFromRequestToGrpcGrpcMdcContextServerServerInterceptor build() {
			if (enricher == null) {
				enricher = (s, m, mdcContext) -> {
					String header = m.get(GrpcTraceMdcContext.CORRELATION_ID_HEADER_KEY);

					if (header != null) {
						String correlationId = GrpcTraceMdcContext.sanitize(header);
						if(correlationId != null) {
							mdcContext.put(GrpcTraceMdcContext.CORRELATION_ID_MDC_KEY, correlationId);
						} else {
							// TODO invalid
							Status status = Status.newBuilder().setCode(Code.INVALID_ARGUMENT_VALUE).setMessage("x-correlation-id header is invalid").build();
							StatusException statusException = StatusProto.toStatusException(status);

							throw new StatusRuntimeException(statusException.getStatus(), statusException.getTrailers());
						}
					} else if(required) {
						Status status = Status.newBuilder().setCode(Code.INVALID_ARGUMENT_VALUE).setMessage("x-correlation-id header is required").build();
						StatusException statusException = StatusProto.toStatusException(status);

						throw new StatusRuntimeException(statusException.getStatus(), statusException.getTrailers());
					} else {
						mdcContext.put(GrpcTraceMdcContext.CORRELATION_ID_MDC_KEY, UUID.randomUUID().toString());
					}

					mdcContext.put(GrpcTraceMdcContext.REQUEST_ID_MDC_KEY, UUID.randomUUID().toString());
				};
			}

			return new CopyTraceFromRequestToGrpcGrpcMdcContextServerServerInterceptor(enricher);
		}

		public Builder withCreateCorrelationIdIfMissing(boolean createIfMissing) {
			this.createCorrelationIdIfMissing = createIfMissing;
			return this;
		}

		public Builder withEnricher(EnrichGrpcMdcContextServerInterceptor enricher) {
			this.enricher = enricher;

			return this;
		}
	}

	protected CopyTraceFromRequestToGrpcGrpcMdcContextServerServerInterceptor(EnrichGrpcMdcContextServerInterceptor enricher) {
		super(enricher);
	}

}
