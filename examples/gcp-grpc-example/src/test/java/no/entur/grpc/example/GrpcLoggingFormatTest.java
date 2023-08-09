package no.entur.grpc.example;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import org.entur.grpc.example.GreetingResponse;
import org.entur.grpc.example.GreetingServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static com.google.common.truth.Truth.assertThat;

@SpringBootTest
public class GrpcLoggingFormatTest extends AbstractGrpcTest {

	@Test
	public void useHumanReadablePlainEncoderTest() {
		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		GreetingResponse response = stub.greeting1(greetingRequest);
		assertThat(response.getMessage()).isEqualTo("Hello");
	}

	@Test 
	public void useHumanReadableJsonEncoderTest() throws InterruptedException {
		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
			GreetingResponse response = stub.greeting1(greetingRequest);
			assertThat(response.getMessage()).isEqualTo("Hello");
		}
	}

	@Test
	public void useMachineReadableJsonEncoder() throws InterruptedException {
		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
			GreetingResponse response = stub.greeting1(greetingRequest);
			assertThat(response.getMessage()).isEqualTo("Hello");
		}
	}

}