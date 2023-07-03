package org.entur.logging.grpc.mdc;


import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.entur.oidc.grpc.test.GreetingResponse;
import org.entur.oidc.grpc.test.GreetingServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class GreetingController extends GreetingServiceGrpc.GreetingServiceImplBase {

	private static final Logger log = LoggerFactory.getLogger(GreetingController.class);

	private final AtomicLong counter = new AtomicLong();

	public void greeting1(org.entur.oidc.grpc.test.GreetingRequest request,
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

	public void exceptionLogging(org.entur.oidc.grpc.test.GreetingRequest request, io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {
		Status status = Status.INVALID_ARGUMENT.withDescription("Mock exception");
		throw status.asRuntimeException();
	}

	/**
	 * Multiple responses
	 */

	public void greeting3(org.entur.oidc.grpc.test.GreetingRequest request,
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

	public void noLogging(org.entur.oidc.grpc.test.GreetingRequest request,
						  io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		log.info("Hello no logging");

		responseObserver.onNext(createResponse(request));
		responseObserver.onCompleted();
	}

	public void fullLogging(org.entur.oidc.grpc.test.GreetingRequest request,
						  io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		log.info("Hello full logging");

		responseObserver.onNext(createResponse(request));
		responseObserver.onCompleted();
	}

	public void summaryLogging(org.entur.oidc.grpc.test.GreetingRequest request,
							io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		log.info("Hello summary logging");

		responseObserver.onNext(createResponse(request));
		responseObserver.onCompleted();
	}

	protected GreetingResponse createResponse(org.entur.oidc.grpc.test.GreetingRequest request) {
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

	public void greetingWithResponseObserverOnErrorCall(org.entur.oidc.grpc.test.GreetingRequest request,
														io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {
		responseObserver.onError(new StatusRuntimeException(Status.INTERNAL));
	}

	public void greeting5(org.entur.oidc.grpc.test.GreetingRequest request,
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