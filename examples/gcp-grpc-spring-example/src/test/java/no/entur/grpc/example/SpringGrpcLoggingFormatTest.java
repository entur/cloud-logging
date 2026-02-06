package no.entur.grpc.example;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
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
	public void useHumanReadablePlainEncoderTest() throws InterruptedException {
		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse response = stub.greeting1(greetingRequest);
			assertThat(response.getMessage()).isEqualTo("Hello");
		} finally {
			shutdown(stub);
		}
	}

	@Test 
	public void useHumanReadableJsonEncoderTest() throws InterruptedException {
		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
			GreetingResponse response = stub.greeting1(greetingRequest);
			assertThat(response.getMessage()).isEqualTo("Hello");
		} finally {
			shutdown(stub);
		}
	}

	@Test
	public void useMachineReadableJsonEncoder() throws InterruptedException {
		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
			GreetingResponse response = stub.greeting1(greetingRequest);
			assertThat(response.getMessage()).isEqualTo("Hello");
		} finally {
			shutdown(stub);
		}
	}

}