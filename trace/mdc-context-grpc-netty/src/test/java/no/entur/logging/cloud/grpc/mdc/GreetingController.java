package no.entur.logging.cloud.grpc.mdc;


import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import no.entur.logging.cloud.grpc.mdc.test.GreetingResponse;
import no.entur.logging.cloud.grpc.mdc.test.GreetingServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class GreetingController extends GreetingServiceGrpc.GreetingServiceImplBase {

	private static final Logger log = LoggerFactory.getLogger(GreetingController.class);

	private final AtomicLong counter = new AtomicLong();

	public void greeting1(no.entur.logging.cloud.grpc.mdc.test.GreetingRequest request,
						  io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		MDC.put("localKey", "valueFromMDC");
		try {
			log.info("Hello greeting 1");
		} finally {
			MDC.remove("localKey");
		}

		responseObserver.onNext(createResponse(request));
		responseObserver.onCompleted();
	}

	public void exceptionLogging(no.entur.logging.cloud.grpc.mdc.test.GreetingRequest request, io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {
		Status status = Status.INVALID_ARGUMENT.withDescription("Mock exception");
		throw status.asRuntimeException();
	}

	/**
	 * Multiple responses
	 */

	public void greeting3(no.entur.logging.cloud.grpc.mdc.test.GreetingRequest request,
						  io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		String traceId = UUID.randomUUID().toString();
		Metadata metadata = new Metadata();
		metadata.put(Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER), traceId);

		log.info("Hello greeting 3");
		for (int i = 0; i < 100; i++) {
			responseObserver.onNext(GreetingResponse.newBuilder().setMessage("Hello " + i).setStatus(counter.getAndIncrement()).build());
		}
		responseObserver.onCompleted();
	}

	public void noLogging(no.entur.logging.cloud.grpc.mdc.test.GreetingRequest request,
						  io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		log.info("Hello no logging");

		responseObserver.onNext(createResponse(request));
		responseObserver.onCompleted();
	}

	public void fullLogging(no.entur.logging.cloud.grpc.mdc.test.GreetingRequest request,
						  io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		log.info("Hello full logging");

		responseObserver.onNext(createResponse(request));
		responseObserver.onCompleted();
	}

	public void summaryLogging(no.entur.logging.cloud.grpc.mdc.test.GreetingRequest request,
							io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		log.info("Hello summary logging");

		responseObserver.onNext(createResponse(request));
		responseObserver.onCompleted();
	}

	protected GreetingResponse createResponse(no.entur.logging.cloud.grpc.mdc.test.GreetingRequest request) {
		StringBuilder builder = new StringBuilder("Hello");

		long size = request.getReturnMessageSize();
		if (size > 0) {
			builder.append(' ');
			for (long i = 0; i < request.getReturnMessageSize(); i++) {
				builder.append((char) ('a' + (int) (i % 27)));
			}
		}

		return GreetingResponse.newBuilder().setMessage(builder.toString()).setStatus(counter.incrementAndGet()).build();
	}

	public void greetingWithResponseObserverOnErrorCall(no.entur.logging.cloud.grpc.mdc.test.GreetingRequest request,
														io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {
		responseObserver.onError(new StatusRuntimeException(Status.INTERNAL));
	}

	public void greeting5(no.entur.logging.cloud.grpc.mdc.test.GreetingRequest request,
						  io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		MDC.put("localKey", "value");
		try {
			log.info("Hello greeting 5");
		} finally {
			MDC.remove("localKey");
		}

		GreetingResponse response = createResponse(request);

		// return same timestamp

		responseObserver.onNext(GreetingResponse.newBuilder(response).setTimestamp(request.getTimestamp()).build());
		responseObserver.onCompleted();
	}

}