package org.entur.logging.grpc.mdc;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.entur.oidc.grpc.test.GreetingRequest;
import org.entur.oidc.grpc.test.GreetingResponse;
import org.entur.oidc.grpc.test.GreetingServiceGrpc.GreetingServiceBlockingStub;
import org.entur.oidc.grpc.test.GreetingServiceGrpc.GreetingServiceFutureStub;
import org.entur.oidc.grpc.test.GreetingServiceGrpc.GreetingServiceStub;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * 
 * Test logging (manual verification).
 *
 */
public class GreetingTest extends AbstractGrpcTest {

	@Test 
	public void testBlockingRequestsOnSameStub() throws InterruptedException {
		GreetingServiceBlockingStub stub = stub();
		// run a few times to see that no unintented state is kept around
		for(int i = 0; i < 100; i++) {
			GreetingResponse response = stub.greeting1(greetingRequest);
			assertThat(response.getMessage()).isEqualTo("Hello");
		}
	}
	

	@Test 
	public void testBlockingRequestsOnNewStubs() throws InterruptedException {
		
		for(int i = 0; i < 100; i++) {
			GreetingServiceBlockingStub stub = stub();
			GreetingResponse response = stub.greeting1(greetingRequest);
			assertThat(response.getMessage()).isEqualTo("Hello");
		}
	}	
	
	@Test
	public void testAsyncRequests() throws InterruptedException {
		// note: so this increments the message counter in the interceptor 
		GreetingServiceStub stub = async();

		Semaphore semaphore = new Semaphore(1);
		semaphore.acquire();
		final List<String> responses = new ArrayList<String>();
		stub.greeting3(greetingRequest, new StreamObserver<GreetingResponse>() {
			@Override
			public void onNext(GreetingResponse response) {
				synchronized(responses) {
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
	}

	@Test
	public void testBlockingRequestWithException() {
		Throwable exception = assertThrows(StatusRuntimeException.class,
				() -> {
					GreetingServiceBlockingStub stub = stub();
					GreetingResponse response = stub.exceptionLogging(greetingRequest);
				});

		assertThat(exception).isInstanceOf(StatusRuntimeException.class);
	}
	
	@Test 
	public void testFutureRequestWithSameStub() throws InterruptedException {
		int n = Math.min(4, Runtime.getRuntime().availableProcessors() * 2);

		GreetingServiceFutureStub stub = futureStub();
		
		CountDownLatch countDownLatch = new CountDownLatch(n);
		List<Thread> workers = Stream
		  .generate(() -> new Thread(new Worker(stub, countDownLatch)))
		  .limit(n)
		  .collect(Collectors.toList());
	 
		  workers.forEach(Thread::start);
		  assertTrue(countDownLatch.await(60, TimeUnit.SECONDS)); 
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
		private GreetingServiceFutureStub stub;
		private CountDownLatch countDownLatch;
	 
		public Worker(GreetingServiceFutureStub stub, CountDownLatch countDownLatch) {
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
			}

		}
	}

	@Test
	public void testStatusDetailsLogged() {
		// validation errors added by interceptor
		Throwable exception = assertThrows(StatusRuntimeException.class,
				() -> {
					GreetingServiceBlockingStub stub = stub();
					GreetingResponse response = stub.greeting4(greetingRequest);
				});

		assertThat(exception).isInstanceOf(StatusRuntimeException.class);
	}

	@Test
	public void testFilteredPathWithNoLogging() throws InterruptedException {
		GreetingServiceBlockingStub stub = stub();
		GreetingResponse response = stub.noLogging(greetingRequest);
		assertThat(response.getMessage()).isEqualTo("Hello");
	}

	@Test
	public void testFilteredPathWithFullLogging() throws InterruptedException {
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().build();

		GreetingServiceBlockingStub stub = stub();
		GreetingResponse response = stub.fullLogging(greetingRequest);
		assertThat(response.getMessage()).isEqualTo("Hello");
	}

	@Test
	public void testFilteredPathWithFullLoggingAndLargeInboundPayload() throws InterruptedException {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < maxInboundMessageSize; i++) {
			builder.append('a' + i % 27);
		}
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setMessage(builder.toString()).build();

		GreetingServiceBlockingStub stub = stub();
		GreetingResponse r = stub.fullLogging(greetingRequest);
		assertThat(r.getMessage()).isEqualTo("Hello");
	}

	@Test
	public void testFilteredPathWithFullLoggingAndLargeOutboundPayload() throws InterruptedException {
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setReturnMessageSize(maxOutboundMessageSize).build();

		GreetingServiceBlockingStub stub = stub();
		GreetingResponse r = stub.fullLogging(greetingRequest);
		assertThat(r.getMessage().length()).isAtLeast(maxOutboundMessageSize);
	}

	@Test
	public void testFilteredPathWithFullLoggingAndLargeInboundAndOutboundPayload() throws InterruptedException {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < maxInboundMessageSize; i++) {
			builder.append('a' + i % 27);
		}
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setReturnMessageSize(maxOutboundMessageSize).setMessage(builder.toString()).build();

		GreetingServiceBlockingStub stub = stub();
		GreetingResponse r = stub.fullLogging(greetingRequest);
		assertThat(r.getMessage().length()).isAtLeast(maxOutboundMessageSize);
	}

	@Test
	public void testFilteredPathWithSummaryLogging() throws InterruptedException {
		GreetingServiceBlockingStub stub = stub();
		GreetingResponse response = stub.summaryLogging(greetingRequest);
		assertThat(response.getMessage()).isEqualTo("Hello");
	}

	@Test
	public void testDefaultLoggingAndLargeInboundPayload() throws InterruptedException {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < maxInboundMessageSize; i++) {
			builder.append('a' + i % 27);
		}
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setMessage(builder.toString()).build();

		GreetingServiceBlockingStub stub = stub();
		GreetingResponse r = stub.greeting1(greetingRequest);
		assertThat(r.getMessage()).isEqualTo("Hello");
	}

	@Test
	public void testDefaultLoggingAndLargeOutboundPayload() throws InterruptedException {
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setReturnMessageSize(maxOutboundMessageSize).build();

		GreetingServiceBlockingStub stub = stub();
		GreetingResponse r = stub.greeting1(greetingRequest);
		assertThat(r.getMessage().length()).isAtLeast(maxOutboundMessageSize);
	}

	@Test
	public void testDefaultLoggingAndLargeInboundAndOutboundPayload() throws InterruptedException {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < maxInboundMessageSize; i++) {
			builder.append('a' + i % 27);
		}
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setReturnMessageSize(maxOutboundMessageSize).setMessage(builder.toString()).build();

		GreetingServiceBlockingStub stub = stub();
		GreetingResponse r = stub.greeting1(greetingRequest);
		assertThat(r.getMessage().length()).isAtLeast(maxOutboundMessageSize);
	}

	@Test
	public void testStatusDetailsLoggedOnResponseObserverOnErrorCall() {
		// validation errors added by interceptor

		// empty payload - does not log body
		assertThrows(StatusRuntimeException.class,
				() -> {
					GreetingServiceBlockingStub stub = stub();
					GreetingResponse response = stub.greetingWithResponseObserverOnErrorCall(greetingRequest);
				});

		// non-empty payload - logs body
		assertThrows(StatusRuntimeException.class,
				() -> {
					GreetingServiceBlockingStub stub = stub();
					GreetingResponse response = stub.greetingWithResponseObserverOnErrorCall(GreetingRequest.newBuilder().setMessage("my message").build());
				});
	}

	@Test
	public void testBlowUpJsonFormatting() throws InterruptedException {
		GreetingRequest greetingRequest = GreetingRequest.newBuilder().setTimestamp(Timestamp.newBuilder().setNanos(Integer.MAX_VALUE).build()).build();

		GreetingServiceBlockingStub stub = stub();
		GreetingResponse response = stub.greeting5(greetingRequest);
		assertThat(response.getMessage()).isEqualTo("Hello");
	}

}
