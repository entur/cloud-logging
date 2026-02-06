package no.entur.grpc.example.without.test.artifacts;

import org.entur.grpc.example.GreetingResponse;
import org.entur.grpc.example.GreetingServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static com.google.common.truth.Truth.assertThat;

@SpringBootTest
@DirtiesContext
public class SpringGrpcLoggingFormatTest extends SpringAbstractGrpcTest {

	@Test
	public void useMachineReadableJsonEncoder() throws InterruptedException {
		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse response = stub.greeting1(greetingRequest);
			assertThat(response.getMessage()).isEqualTo("Hello");
		} finally {
			shutdown(stub);
		}
	}

}