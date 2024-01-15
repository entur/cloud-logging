package no.entur.grpc.example;

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

/**
 *
 * Test additional logging due to a log statement with high log level.
 *
 */

@SpringBootTest
@ActiveProfiles("ondemand")
@TestPropertySource(properties = {
		"entur.logging.grpc.ondemand.enabled=true",
		"entur.logging.http.ondemand.failure.logger.level=error",
})
@DirtiesContext
public class OndemandGrpcLoggingHighLogLevelTest extends AbstractGrpcTest {

	@Test
	public void useMachineReadableJsonEncoderExpectFullLogging() throws InterruptedException {
		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse response = stub.greeting1(greetingRequest);
			assertThat(response.getMessage()).isEqualTo("Hello");
		} finally {
			shutdown(stub);
		}
	}

}