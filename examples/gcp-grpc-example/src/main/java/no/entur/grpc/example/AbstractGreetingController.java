package no.entur.grpc.example;


import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.entur.grpc.example.GreetingResponse;
import org.entur.grpc.example.GreetingServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class AbstractGreetingController extends GreetingServiceGrpc.GreetingServiceImplBase {

	private static final Logger log = LoggerFactory.getLogger(AbstractGreetingController.class);

	private final AtomicLong counter = new AtomicLong();

	public void greeting1(org.entur.grpc.example.GreetingRequest request,
						  io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		MDC.put("localKey", "value");
		try {
			log.trace("Hello greeting / trace");
			log.debug("Hello greeting / debug");
			log.info("Hello greeting / info");
			log.warn("Hello greeting / warn");
		} finally {
			MDC.remove("localKey");
		}

		responseObserver.onNext(createResponse(request));
		responseObserver.onCompleted();
	}

	public void exceptionLogging(org.entur.grpc.example.GreetingRequest request, io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {
		MDC.put("localKey", "value");
		try {
			log.trace("Hello error / trace");
			log.debug("Hello error / debug");
			log.info("Hello error / info");
			log.warn("Hello error / warn");
			log.error("Hello error / error");
		} finally {
			MDC.remove("localKey");
		}

		Status status = Status.INVALID_ARGUMENT.withDescription("Mock exception");
		throw status.asRuntimeException();
	}

	/**
	 * Multiple responses
	 */

	public void greeting3(org.entur.grpc.example.GreetingRequest request,
						  io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		String traceId = UUID.randomUUID().toString();
		Metadata metadata = new Metadata();
		metadata.put(Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER), traceId);

		log.trace("Hello greeting 3 / trace");
		log.debug("Hello greeting 3 / debug");
		log.info("Hello greeting 3 / info");
		log.warn("Hello greeting 3 / warn");
		for (int i = 0; i < 100; i++) {
			responseObserver.onNext(GreetingResponse.newBuilder().setMessage("Hello " + i).setStatus(counter.getAndIncrement()).build());
		}
		responseObserver.onCompleted();
	}

	public void noLogging(org.entur.grpc.example.GreetingRequest request,
						  io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		log.trace("Hello no logging / trace");
		log.debug("Hello no logging / debug");
		log.info("Hello no logging / info");
		log.warn("Hello no logging / warn");

		responseObserver.onNext(createResponse(request));
		responseObserver.onCompleted();
	}

	public void fullLogging(org.entur.grpc.example.GreetingRequest request,
						  io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		log.trace("Hello full logging");
		log.debug("Hello full logging");
		log.info("Hello full logging");
		log.warn("Hello full logging");

		responseObserver.onNext(createResponse(request));
		responseObserver.onCompleted();
	}

	public void summaryLogging(org.entur.grpc.example.GreetingRequest request,
							io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		log.trace("Hello summary logging");
		log.debug("Hello summary logging");
		log.info("Hello summary logging");
		log.warn("Hello summary logging");

		responseObserver.onNext(createResponse(request));
		responseObserver.onCompleted();
	}

	protected GreetingResponse createResponse(org.entur.grpc.example.GreetingRequest request) {
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

	public void greetingWithResponseObserverOnErrorCall(org.entur.grpc.example.GreetingRequest request,
														io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		MDC.put("localKey", "value");
		try {
			log.trace("Hello error / trace");
			log.debug("Hello error / debug");
			log.info("Hello error / info");
			log.warn("Hello error / warn");
			log.error("Hello error / error");
		} finally {
			MDC.remove("localKey");
		}

		responseObserver.onError(new StatusRuntimeException(Status.INTERNAL));
	}

	public void greeting5(org.entur.grpc.example.GreetingRequest request,
						  io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		MDC.put("localKey", "value");
		try {
			log.trace("Hello greeting 5");
			log.debug("Hello greeting 5");
			log.info("Hello greeting 5");
			log.warn("Hello greeting 5");
		} finally {
			MDC.remove("localKey");
		}

		GreetingResponse response = createResponse(request);

		// return same timestamp

		responseObserver.onNext(GreetingResponse.newBuilder(response).setTimestamp(request.getTimestamp()).build());
		responseObserver.onCompleted();
	}

}