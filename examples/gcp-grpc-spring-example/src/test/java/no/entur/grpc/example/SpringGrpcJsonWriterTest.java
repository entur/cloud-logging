package no.entur.grpc.example;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import no.entur.logging.cloud.protobuf.json.ProtobufJsonWriter;
import no.entur.logging.cloud.protobuf.json.codedoutputstream.CodedOutputStreamProtobufJsonWriter;
import org.entur.grpc.example.GreetingResponse;
import org.entur.grpc.example.GreetingServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static com.google.common.truth.Truth.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
		"entur.logging.request-response.grpc.json-writer=coded-output-stream"
})
@DirtiesContext
public class SpringGrpcJsonWriterTest extends SpringAbstractGrpcTest {

	@Autowired
	private ProtobufJsonWriter protobufJsonWriter;

	@Test
	public void useCodedOutputStreamJsonWriter() throws InterruptedException {
		assertThat(protobufJsonWriter).isInstanceOf(CodedOutputStreamProtobufJsonWriter.class);
		// running on Java 24+
		assertThat(CodedOutputStreamProtobufJsonWriter.getUnavailableCause()).isNull();

		GreetingServiceGrpc.GreetingServiceBlockingStub stub = stub();
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
			GreetingResponse response = stub.greeting1(greetingRequest);
			assertThat(response.getMessage()).isEqualTo("Hello");
		} finally {
			shutdown(stub);
		}
	}
}
