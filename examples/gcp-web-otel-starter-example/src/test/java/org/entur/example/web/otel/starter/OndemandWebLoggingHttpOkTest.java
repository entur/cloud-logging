package org.entur.example.web.otel.starter;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import no.entur.logging.cloud.logback.logstash.test.junit.CaptureLogStatements;
import no.entur.logging.cloud.logback.logstash.test.junit.LogStatements;
import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static com.google.common.truth.Truth.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)

// Triggers by error log level or >= 400 status code by default

@TestPropertySource(properties = {"entur.logging.http.ondemand.enabled=true"})
@AutoConfigureTestRestTemplate
@CaptureLogStatements({"no.entur", "org.entur"})
public class OndemandWebLoggingHttpOkTest {

	@LocalServerPort
	private int randomServerPort;
	
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void useHumanReadablePlainEncoderExpectReducedLogging(LogStatements statements) {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method/infoLoggingOnly", entity, MyEntity.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

		WebLoggingFormatTest.assertGcpTrace(statements);
	}

	@Test
	public void useHumanReadableJsonEncoderExpectReducedLogging(LogStatements statements) throws InterruptedException {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
			ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method/infoLoggingOnly", entity, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}

		WebLoggingFormatTest.assertGcpTrace(statements);
	}

	@Test
	public void useMachineReadableJsonEncoderExpectReducedLogging(LogStatements statements) throws InterruptedException {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
			ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method/infoLoggingOnly", entity, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}
		WebLoggingFormatTest.assertGcpTrace(statements);
	}


}