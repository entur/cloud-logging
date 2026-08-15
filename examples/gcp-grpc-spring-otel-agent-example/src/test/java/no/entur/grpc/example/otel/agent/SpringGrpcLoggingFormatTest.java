package no.entur.grpc.example.otel.agent;

import no.entur.logging.cloud.logback.logstash.test.junit.CaptureLogStatements;
import no.entur.logging.cloud.logback.logstash.test.junit.LogStatements;
import org.entur.grpc.example.GreetingResponse;
import org.entur.grpc.example.GreetingServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static com.google.common.truth.Truth.assertThat;

@SpringBootTest
@DirtiesContext
@CaptureLogStatements({"no.entur", "org.entur"})
public class SpringGrpcLoggingFormatTest extends SpringAbstractGrpcTest {

	@Test
	public void useMachineReadableJsonEncoder(LogStatements logStatements) throws InterruptedException {
		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try {
			GreetingResponse response = stub.greeting1(greetingRequest);
			assertThat(response.getMessage()).isEqualTo("Hello");

			assertGcpTrace(logStatements);
		} finally {
			shutdown(stub);
		}
	}

}