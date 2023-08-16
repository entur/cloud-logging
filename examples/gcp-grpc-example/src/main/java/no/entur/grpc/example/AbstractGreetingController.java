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

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGreetingController.class);

	private final AtomicLong counter = new AtomicLong();

	public void greeting1(org.entur.grpc.example.GreetingRequest request,
						  io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		MDC.put("localKey", "value");
		try {
			LOGGER.trace("Hello greeting / trace");
			LOGGER.debug("Hello greeting / debug");
			LOGGER.info("Hello greeting / info");
			LOGGER.warn("Hello greeting / warn");
		} finally {
			MDC.remove("localKey");
		}

		responseObserver.onNext(createResponse(request));
		responseObserver.onCompleted();
	}

	public void exceptionLogging(org.entur.grpc.example.GreetingRequest request, io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {
		MDC.put("localKey", "value");
		try {

			System.out.flush();
			System.out.println("System out before endpoint logging");

			LOGGER.trace("This message should be ignored / trace");
			LOGGER.debug("This message should be ignored / debug");
			LOGGER.info("This message should be delayed / info");
			LOGGER.warn("This message should be logged / warn");
			LOGGER.error("This message should be logged / error");

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			System.out.println("System out after endpoint logging + 1000ms");

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

		LOGGER.trace("Hello greeting 3 / trace");
		LOGGER.debug("Hello greeting 3 / debug");
		LOGGER.info("Hello greeting 3 / info");
		LOGGER.warn("Hello greeting 3 / warn");
		for (int i = 0; i < 100; i++) {
			responseObserver.onNext(GreetingResponse.newBuilder().setMessage("Hello " + i).setStatus(counter.getAndIncrement()).build());
		}
		responseObserver.onCompleted();
	}

	public void noLogging(org.entur.grpc.example.GreetingRequest request,
						  io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		LOGGER.trace("Hello no logging / trace");
		LOGGER.debug("Hello no logging / debug");
		LOGGER.info("Hello no logging / info");
		LOGGER.warn("Hello no logging / warn");

		responseObserver.onNext(createResponse(request));
		responseObserver.onCompleted();
	}

	public void fullLogging(org.entur.grpc.example.GreetingRequest request,
						  io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		LOGGER.trace("Hello full logging");
		LOGGER.debug("Hello full logging");
		LOGGER.info("Hello full logging");
		LOGGER.warn("Hello full logging");

		responseObserver.onNext(createResponse(request));
		responseObserver.onCompleted();
	}

	public void summaryLogging(org.entur.grpc.example.GreetingRequest request,
							io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		LOGGER.trace("Hello summary logging");
		LOGGER.debug("Hello summary logging");
		LOGGER.info("Hello summary logging");
		LOGGER.warn("Hello summary logging");

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
			LOGGER.trace("Hello error / trace");
			LOGGER.debug("Hello error / debug");
			LOGGER.info("Hello error / info");
			LOGGER.warn("Hello error / warn");
			LOGGER.error("Hello error / error");
		} finally {
			MDC.remove("localKey");
		}

		responseObserver.onError(new StatusRuntimeException(Status.INTERNAL));
	}

	public void greeting5(org.entur.grpc.example.GreetingRequest request,
						  io.grpc.stub.StreamObserver<GreetingResponse> responseObserver) {

		MDC.put("localKey", "value");
		try {
			LOGGER.trace("Hello greeting 5");
			LOGGER.debug("Hello greeting 5");
			LOGGER.info("Hello greeting 5");
			LOGGER.warn("Hello greeting 5");
		} finally {
			MDC.remove("localKey");
		}

		GreetingResponse response = createResponse(request);

		// return same timestamp

		responseObserver.onNext(GreetingResponse.newBuilder(response).setTimestamp(request.getTimestamp()).build());
		responseObserver.onCompleted();
	}

}