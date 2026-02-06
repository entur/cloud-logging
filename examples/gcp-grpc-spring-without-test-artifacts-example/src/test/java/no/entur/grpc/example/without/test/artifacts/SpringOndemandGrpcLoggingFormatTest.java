package no.entur.grpc.example.without.test.artifacts;

import io.grpc.StatusRuntimeException;
import org.entur.grpc.example.GreetingResponse;
import org.entur.grpc.example.GreetingServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("ondemand")
@TestPropertySource(properties = {"entur.logging.grpc.ondemand.enabled=true"})
@DirtiesContext
public class SpringOndemandGrpcLoggingFormatTest extends SpringAbstractGrpcTest {

	@Test
	public void useMachineReadableJsonEncoderExpectFullLogging() throws InterruptedException {
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
	public void useMachineReadableJsonEncoderExpectReducedLogging() throws InterruptedException {
		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		GreetingResponse response = stub.greeting1(greetingRequest);
		assertThat(response.getMessage()).isEqualTo("Hello");
	}

}