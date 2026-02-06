package org.entur.example.web;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import no.entur.logging.cloud.spring.ondemand.web.scope.LoggingScopeControls;
import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * Note: Async does not currently work with on-demand logging.
 *
 */

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("async")
@TestPropertySource(properties = {"entur.logging.http.ondemand.enabled=true", "entur.logging.http.ondemand.failure.http.statusCode.equalOrHigherThan=400", "entur.logging.http.ondemand.failure.logger.level=error", "entur.logging.http.ondemand.failure.level=info"})
@AutoConfigureTestRestTemplate
public class OndemandWebLoggingHttpNotFound1AsyncTest {

	@LocalServerPort
	private int randomServerPort;
	
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void useHumanReadablePlainEncoderExpectFullLogging() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/async-document/some/error", entity, MyEntity.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void useHumanReadableJsonEncoderExpectFullLogging() {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
			MyEntity entity = new MyEntity();
			entity.setName("Entur");
			entity.setSecret("mySecret");

			ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/async-document/some/error", entity, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		}
	}

	@Test
	public void useMachineReadableJsonEncoderExpectFullLogging() {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
			MyEntity entity = new MyEntity();
			entity.setName("Entur");
			entity.setSecret("mySecret");

			ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/async-document/some/error", entity, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		}
	}

}