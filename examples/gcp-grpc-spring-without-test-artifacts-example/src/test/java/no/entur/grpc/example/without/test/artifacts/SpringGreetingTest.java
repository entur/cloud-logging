package no.entur.grpc.example.without.test.artifacts;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

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
public class SpringGreetingTest extends SpringAbstractGrpcTest {

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
	public void testAsyncRequests() throws InterruptedException {
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
		} finally {
			shutdown(stub);
		}
	}

	@Test
	public void testBlockingRequestWithException() {
		Throwable exception = assertThrows(StatusRuntimeException.class,
				() -> {
					GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
					try {
						GreetingResponse response = stub.exceptionLogging(greetingRequest);
					} finally {
						shutdown(stub);
					}
				});

		assertThat(exception).isInstanceOf(StatusRuntimeException.class);
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
	public void testStatusDetailsLogged() {
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
	public void testFilteredPathWithFullLoggingAndLargeInboundPayload() throws InterruptedException {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < maxInboundMessageSize; i++) {
			builder.append('a' + i % 27);
		}
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setMessage(builder.toString()).build();

		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse r = stub.fullLogging(greetingRequest);
			assertThat(r.getMessage()).isEqualTo("Hello");
		} finally {
			shutdown(stub);
		}
	}

	@Test
	public void testFilteredPathWithFullLoggingAndLargeOutboundPayload() throws InterruptedException {
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setReturnMessageSize(maxOutboundMessageSize).build();

		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse r = stub.fullLogging(greetingRequest);
			assertThat(r.getMessage().length()).isAtLeast(maxOutboundMessageSize);
		} finally {
			shutdown(stub);
		}
	}

	@Test
	public void testFilteredPathWithFullLoggingAndLargeInboundAndOutboundPayload() throws InterruptedException {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < maxInboundMessageSize; i++) {
			builder.append('a' + i % 27);
		}
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setReturnMessageSize(maxOutboundMessageSize).setMessage(builder.toString()).build();

		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse r = stub.fullLogging(greetingRequest);
			assertThat(r.getMessage().length()).isAtLeast(maxOutboundMessageSize);
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
	public void testDefaultLoggingAndLargeInboundPayload() throws InterruptedException {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < maxInboundMessageSize; i++) {
			builder.append('a' + i % 27);
		}
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setMessage(builder.toString()).build();

		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse r = stub.greeting1(greetingRequest);
			assertThat(r.getMessage()).isEqualTo("Hello");
		} finally {
			shutdown(stub);
		}
	}

	@Test
	public void testDefaultLoggingAndLargeOutboundPayload() throws InterruptedException {
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setReturnMessageSize(maxOutboundMessageSize).build();

		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse r = stub.greeting1(greetingRequest);
			assertThat(r.getMessage().length()).isAtLeast(maxOutboundMessageSize);
		} finally {
			shutdown(stub);
		}
	}

	@Test
	public void testDefaultLoggingAndLargeInboundAndOutboundPayload() throws InterruptedException {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < maxInboundMessageSize; i++) {
			builder.append('a' + i % 27);
		}
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setReturnMessageSize(maxOutboundMessageSize).setMessage(builder.toString()).build();

		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse r = stub.greeting1(greetingRequest);
			assertThat(r.getMessage().length()).isAtLeast(maxOutboundMessageSize);
		} finally {
			shutdown(stub);
		}
	}

	@Test
	public void testBlowUpJsonFormatting() throws InterruptedException {
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setTimestamp(Timestamp.newBuilder().setNanos(Integer.MAX_VALUE).build()).build();

		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse response = stub.greeting5(greetingRequest);
			assertThat(response.getMessage()).isEqualTo("Hello");
		} finally {
			shutdown(stub);
		}
	}

}
