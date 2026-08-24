package org.entur.example.web.otel.agent;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import no.entur.logging.cloud.logback.logstash.test.junit.CaptureLogStatements;
import no.entur.logging.cloud.logback.logstash.test.junit.LogStatements;
import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static com.google.common.truth.Truth.assertThat;

/**
 *
 * Test additional logging due to a log statement with high log level.
 *
 */

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
		"entur.logging.http.ondemand.enabled=true",
		"entur.logging.http.ondemand.failure.http.enabled=false",
		"entur.logging.http.ondemand.failure.logger.level=error",
})
@AutoConfigureTestRestTemplate
@CaptureLogStatements({"no.entur", "org.entur"})
public class OndemandWebLoggingHttpOkHighLogLevelTest {

	@LocalServerPort
	private int randomServerPort;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void useHumanReadablePlainEncoderExpectFullLogging(LogStatements statements) {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method", entity, MyEntity.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		WebLoggingFormatTest.assertGcpTrace(statements);
	}

	@Test
	public void useHumanReadableJsonEncoderExpectFullLogging(LogStatements statements) {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
			ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method", entity, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}

		WebLoggingFormatTest.assertGcpTrace(statements);
	}

	@Test
	public void useMachineReadableJsonEncoderExpectFullLogging(LogStatements statements) {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
			ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method", entity, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}
		WebLoggingFormatTest.assertGcpTrace(statements);
	}

}