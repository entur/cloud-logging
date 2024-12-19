package no.entur.grpc.azure.example;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import no.entur.logging.cloud.api.DevOpsLevel;
import no.entur.logging.cloud.grpc.trace.CorrelationIdGrpcMdcContext;
import no.entur.logging.cloud.logback.logstash.test.junit.CaptureLogStatements;
import no.entur.logging.cloud.logback.logstash.test.junit.LogStatement;
import no.entur.logging.cloud.logback.logstash.test.junit.LogStatements;
import no.entur.logging.cloud.rr.grpc.GrpcLoggingServerInterceptor;
import org.entur.grpc.example.GreetingRequest;
import org.entur.grpc.example.GreetingResponse;
import org.entur.grpc.example.GreetingServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * 
 * Test logging (manual verification).
 *
 */
@SpringBootTest
@DirtiesContext
@CaptureLogStatements(level = DevOpsLevel.DEBUG, value = {"no.entur", "org.entur"})
public class EcosystemGreetingTest extends EcosystemAbstractGrpcTest {

	@Test 
	public void testBlockingRequestsOnSameStub() throws InterruptedException {
		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		// run a few times to see that no unintented state is kept around
		try {
			for (int i = 0; i < 100; i++) {
				GreetingResponse response = stub.greeting1(greetingRequest);
				assertThat(response.getMessage()).isEqualTo("Hello");
			}
		} finally {
			shutdown(stub);
		}
	}

	@Test 
	public void testBlockingRequestsOnNewStubs() throws InterruptedException {
		for(int i = 0; i < 100; i++) {
			GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
			try {
				GreetingResponse response = stub.greeting1(greetingRequest);
				assertThat(response.getMessage()).isEqualTo("Hello");
			} finally {
				shutdown(stub);
			}
		}
	}
	
	@Test
	public void testAsyncRequests(LogStatements statements) throws InterruptedException {
		// note: so this increments the message counter in the interceptor 
		GreetingServiceGrpc.GreetingServiceStub stub = async();

		try {
			Semaphore semaphore = new Semaphore(1);
			semaphore.acquire();
			final List<String> responses = new ArrayList<String>();
			stub.greeting3(greetingRequest, new StreamObserver<GreetingResponse>() {
				@Override
				public void onNext(GreetingResponse response) {
					synchronized (responses) {
						responses.add(response.getMessage());
					}
				}

				@Override
				public void onError(Throwable throwable) {
					semaphore.release();
				}

				@Override
				public void onCompleted() {
					semaphore.release();
				}
			});
			semaphore.acquireUninterruptibly();

			assertThat(responses.size()).isEqualTo(100);
			for (int i = 0; i < 100; i++) {
				assertThat(responses.get(i)).isEqualTo("Hello " + i);
			}

			LogStatements http = statements.forLogger("no.entur.logging.cloud");
			LogStatement request = http.get(0);

			String s = request.getMdc().get(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY);

			for (LogStatement statement : statements) {

				// check that correlation-id was set back as a header in both request and response
				// and that it was logged
				assertEquals(statement.getMdc().get(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY), s);
				statement.assertThatHttpHeader(CorrelationIdGrpcMdcContext.CORRELATION_ID_HEADER.toLowerCase()).contains(s);
			}
		} finally {
			shutdown(stub);
		}
	}

	@Test
	public void testBlockingRequestWithStatusRuntimeException(LogStatements statements) {
		Throwable exception = assertThrows(StatusRuntimeException.class,
			() -> {
				GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
				try {
					GreetingResponse response = stub.statusRuntimeExceptionLogging(greetingRequest);
				} finally {
					shutdown(stub);
				}
			});

		assertThat(exception).isInstanceOf(StatusRuntimeException.class);

		LogStatements http = statements.forLogger("no.entur.logging.cloud");
		LogStatement request = http.get(0);
		request.assertThatMessage().matches("REQ /org.entur.grpc.example.GreetingService/statusRuntimeExceptionLogging #1[\\s\\S]*");
		LogStatement response = http.get(http.size() - 1);
		response.assertThatMessage().matches("RESP INVALID_ARGUMENT /org.entur.grpc.example.GreetingService/statusRuntimeExceptionLogging #1[\\s\\S]*");

		response.assertThatHttpHeader("grpc-status").contains("3");

		// check that correlation-id was set back as a header in the response
		response.assertThatHttpHeader(CorrelationIdGrpcMdcContext.CORRELATION_ID_HEADER.toLowerCase()).contains(response.getMdc().get(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY));
	}

	@Test
	public void testBlockingRequestWithRuntimeException(LogStatements statements) {
		Throwable exception = assertThrows(StatusRuntimeException.class,
				() -> {
					GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
					try {
						GreetingResponse response = stub.runtimeExceptionLogging(greetingRequest);
					} finally {
						shutdown(stub);
					}
				});

		assertThat(exception).isInstanceOf(StatusRuntimeException.class);

		LogStatements http = statements.forLogger("no.entur.logging.cloud");
		LogStatement request = http.get(0);
		request.assertThatMessage().matches("REQ /org.entur.grpc.example.GreetingService/runtimeExceptionLogging #1[\\s\\S]*");
		LogStatement response = http.get(http.size() - 1);
		response.assertThatMessage().matches("RESP INTERNAL /org.entur.grpc.example.GreetingService/runtimeExceptionLogging #1[\\s\\S]*");

		response.assertThatHttpHeader("grpc-status").contains("13");

		// check that correlation-id was set back as a header in the response
		response.assertThatHttpHeader(CorrelationIdGrpcMdcContext.CORRELATION_ID_HEADER.toLowerCase()).contains(response.getMdc().get(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY));
	}


	@Test 
	public void testFutureRequestWithSameStub() throws InterruptedException {
		int n = Math.min(4, Runtime.getRuntime().availableProcessors() * 2);

		GreetingServiceGrpc.GreetingServiceFutureStub stub = futureStub();
		try {
			CountDownLatch countDownLatch = new CountDownLatch(n);
			List<Thread> workers = Stream
					.generate(() -> new Thread(new Worker(stub, countDownLatch)))
					.limit(n)
					.collect(Collectors.toList());

			workers.forEach(Thread::start);
			assertTrue(countDownLatch.await(60, TimeUnit.SECONDS));
		} finally {
			shutdown(stub);
		}
	}
	
	@Test 
	public void testFutureRequestWithNewStub() throws InterruptedException {
		int n = Math.min(4, Runtime.getRuntime().availableProcessors() * 2);
		
		CountDownLatch countDownLatch = new CountDownLatch(n);
		List<Thread> workers = Stream
		  .generate(() -> new Thread(new Worker(futureStub(), countDownLatch)))
		  .limit(n)
		  .collect(Collectors.toList());
	 
		  workers.forEach(Thread::start);
		  assertTrue(countDownLatch.await(60, TimeUnit.SECONDS)); 
		
	}
	
	public class Worker implements Runnable {
		private GreetingServiceGrpc.GreetingServiceFutureStub stub;
		private CountDownLatch countDownLatch;
	 
		public Worker(GreetingServiceGrpc.GreetingServiceFutureStub stub, CountDownLatch countDownLatch) {
			this.countDownLatch = countDownLatch;
			this.stub = stub;
		}
	 
		@Override
		public void run() {
			try {
				// run a few times to check that the internal / anonymous classes are not reused
				List<ListenableFuture<GreetingResponse>> list = new ArrayList<>();
	
				for(int i = 0; i < 100; i++) {
					list.add(stub.greeting1(greetingRequest));
				}
				
				while(!list.isEmpty()) {
					ListenableFuture<GreetingResponse> futureResponse = list.get(list.size() - 1);
					try {
						GreetingResponse response = futureResponse.get(10, TimeUnit.SECONDS);
						assertThat(response.getMessage()).isEqualTo("Hello");
						
						list.remove(list.size() - 1);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}

			} finally {
				countDownLatch.countDown();
				try {
					shutdown(stub);
				} catch (InterruptedException e) {
					// ignore
				}
			}

		}
	}

	@Test
	public void testStatusDetailsLogged(LogStatements statements) {
		// validation errors added by interceptor
		Throwable exception = assertThrows(StatusRuntimeException.class,
				() -> {
					GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
					try {
						GreetingResponse response = stub.greeting4(greetingRequest);
					} finally {
						shutdown(stub);
					}
				});

		assertThat(exception).isInstanceOf(StatusRuntimeException.class);

		LogStatements http = statements.forLogger("no.entur.logging.cloud");

		LogStatement request = http.get(0);
		request.assertThatMessage().matches("REQ /org.entur.grpc.example.GreetingService/greeting4 #1[\\s\\S]*");
		LogStatement response = http.get(http.size() - 1);
		response.assertThatMessage().matches("RESP INVALID_ARGUMENT /org.entur.grpc.example.GreetingService/greeting4 #1[\\s\\S]*");

		response.assertThatHttpHeader("grpc-status").contains("3");
		response.assertThatHttpHeader("grpc-message").contains("My error message");
		assertThat(response.getJson().contains("fieldViolations")); // json path with dotted field names = crap

		// check that correlation-id was set back as a header in the response
		response.assertThatHttpHeader(CorrelationIdGrpcMdcContext.CORRELATION_ID_HEADER.toLowerCase()).contains(response.getMdc().get(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY));
	}

	@Test
	public void testFilteredPathWithNoLogging() throws InterruptedException {
		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse response = stub.noLogging(greetingRequest);
			assertThat(response.getMessage()).isEqualTo("Hello");
		} finally {
			shutdown(stub);
		}
	}

	@Test
	public void testFilteredPathWithFullLogging() throws InterruptedException {
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().build();

		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse response = stub.fullLogging(greetingRequest);
			assertThat(response.getMessage()).isEqualTo("Hello");
		} finally {
			shutdown(stub);
		}
	}

	@Test
	public void testFilteredPathWithFullLoggingAndLargeInboundPayload(LogStatements statements) throws InterruptedException {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < maxInboundMessageSize; i++) {
			builder.append('a' + i % 27);
		}
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setMessage(builder.toString()).build();

		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse r = stub.fullLogging(greetingRequest);
			assertThat(r.getMessage()).isEqualTo("Hello");

			LogStatements http = statements.forLogger("no.entur.logging.cloud");
			LogStatement request = http.get(1);
			request.assertThatHttpBody().matches("Omitted binary message size [0-9]+");

			// check that correlation-id was set back as a header in the response
			LogStatement response = http.get(2);
			response.assertThatHttpHeader(CorrelationIdGrpcMdcContext.CORRELATION_ID_HEADER.toLowerCase()).contains(response.getMdc().get(CorrelationIdGrpcMdcContext.CORRELATION_ID_MDC_KEY));
		} finally {
			shutdown(stub);
		}
	}

	@Test
	public void testFilteredPathWithFullLoggingAndLargeOutboundPayload(LogStatements statements) throws InterruptedException {
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setReturnMessageSize(maxOutboundMessageSize).build();

		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse r = stub.fullLogging(greetingRequest);
			assertThat(r.getMessage().length()).isAtLeast(maxOutboundMessageSize);

			LogStatements http = statements.forLogger("no.entur.logging.cloud");
			LogStatement response = http.get(http.size() - 2);
			response.assertThatHttpBody().matches("Omitted binary message size [0-9]+");
		} finally {
			shutdown(stub);
		}
	}

	@Test
	public void testFilteredPathWithFullLoggingAndLargeInboundAndOutboundPayload(LogStatements statements) throws InterruptedException {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < maxInboundMessageSize; i++) {
			builder.append('a' + i % 27);
		}
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setReturnMessageSize(maxOutboundMessageSize).setMessage(builder.toString()).build();

		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse r = stub.fullLogging(greetingRequest);
			assertThat(r.getMessage().length()).isAtLeast(maxOutboundMessageSize);

			LogStatements http = statements.forLogger("no.entur.logging.cloud");
			LogStatement request = http.get(1);
			request.assertThatHttpBody().matches("Omitted binary message size [0-9]+");
			LogStatement response = http.get(http.size() - 2);
			response.assertThatHttpBody().matches("Omitted binary message size [0-9]+");
		} finally {
			shutdown(stub);
		}
	}

	@Test
	public void testFilteredPathWithSummaryLogging() throws InterruptedException {
		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse response = stub.summaryLogging(greetingRequest);
			assertThat(response.getMessage()).isEqualTo("Hello");
		} finally {
			shutdown(stub);
		}
	}

	@Test
	public void testDefaultLoggingAndLargeInboundPayload(LogStatements statements) throws InterruptedException {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < maxInboundMessageSize; i++) {
			builder.append('a' + i % 27);
		}
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setMessage(builder.toString()).build();

		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse r = stub.greeting1(greetingRequest);
			assertThat(r.getMessage()).isEqualTo("Hello");

			LogStatements http = statements.forLogger("no.entur.logging.cloud");
			LogStatement request = http.get(0);
			request.assertThatHttpBody().matches("Omitted binary message size [0-9]+");
		} finally {
			shutdown(stub);
		}
	}

	@Test
	public void testDefaultLoggingAndLargeOutboundPayload(LogStatements statements) throws InterruptedException {
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setReturnMessageSize(maxOutboundMessageSize).build();

		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse r = stub.greeting1(greetingRequest);
			assertThat(r.getMessage().length()).isAtLeast(maxOutboundMessageSize);

			LogStatements http = statements.forLogger("no.entur.logging.cloud");
			LogStatement response = http.get(http.size() - 1);
			response.assertThatHttpBody().matches("Omitted binary message size [0-9]+");
		} finally {
			shutdown(stub);
		}
	}

	@Test
	public void testDefaultLoggingAndLargeInboundAndOutboundPayload(LogStatements statements) throws InterruptedException {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < maxInboundMessageSize; i++) {
			builder.append('a' + i % 27);
		}
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setReturnMessageSize(maxOutboundMessageSize).setMessage(builder.toString()).build();

		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse r = stub.greeting1(greetingRequest);
			assertThat(r.getMessage().length()).isAtLeast(maxOutboundMessageSize);

			LogStatements http = statements.forLogger("no.entur.logging.cloud");
			LogStatement request = http.get(0);
			request.assertThatHttpBody().matches("Omitted binary message size [0-9]+");
			LogStatement response = http.get(http.size() - 1);
			response.assertThatHttpBody().matches("Omitted binary message size [0-9]+");
		} finally {
			shutdown(stub);
		}
	}

	@Test
	public void testBlowUpJsonFormatting(LogStatements statements) throws InterruptedException {
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setTimestamp(Timestamp.newBuilder().setNanos(Integer.MAX_VALUE).build()).build();

		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse response = stub.greeting5(greetingRequest);
			assertThat(response.getMessage()).isEqualTo("Hello");

			List<LogStatement> logStatements = statements.forLogger(GrpcLoggingServerInterceptor.class);
			logStatements.get(0).assertThatField("level").isEqualTo("INFO");
			logStatements.get(1).assertThatField("level").isEqualTo("WARN");
		} finally {
			shutdown(stub);
		}
	}

}
