package org.entur.example.web;

import no.entur.logging.cloud.gcp.logback.logstash.StackdriverMicrometerTraceMdcJsonProvider;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import no.entur.logging.cloud.logback.logstash.test.junit.CaptureLogStatements;
import no.entur.logging.cloud.logback.logstash.test.junit.LogStatement;
import no.entur.logging.cloud.logback.logstash.test.junit.LogStatements;
import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.google.common.truth.Truth.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@CaptureLogStatements({"no.entur", "org.entur"})
public class WebLoggingFormatTest {

	@LocalServerPort
    private int randomServerPort;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void useHumanReadablePlainEncoderTest(LogStatements statements) throws Exception {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method", entity, MyEntity.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		assertGcpTrace(statements);
	}

	@Test 
	public void useHumanReadableJsonEncoderTest(LogStatements statements) throws Exception {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
			ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method", entity, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}

		assertGcpTrace(statements);
	}

	@Test
	public void useMachineReadableJsonEncoder(LogStatements statements) throws Exception {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
			ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method", entity, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}

		assertGcpTrace(statements);
	}

	public static void assertGcpTrace(LogStatements statements) {
		// Wait a bit to ensure that the logs have been flushed and captured
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
		Assertions.assertFalse(statements.isEmpty(), "Expected log statements to be captured, but none were found.");
		for (LogStatement statement : statements) {
			assertThat(statement.getJsonPropertyString(StackdriverMicrometerTraceMdcJsonProvider.GCP_TRACE_KEY)).hasLength(32);
			assertThat(statement.getJsonPropertyString(StackdriverMicrometerTraceMdcJsonProvider.GCP_SPAN_ID_KEY)).hasLength(16);
		}
	}

}