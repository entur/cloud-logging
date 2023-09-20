package org.entur.logbook.example;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import org.entur.logbook.example.rest.MyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * Note: Expect that logged JSON request body is NOT validated; as there is no exception
 *
 */

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"entur.logging.http.ondemand.enabled=true", "entur.logging.http.ondemand.failure.http.statusCode.equalOrHigherThan=400"})
public class OndemandWebLoggingHttpNotFound1Test {

	@LocalServerPort
    private int randomServerPort;
	
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void useHumanReadablePlainEncoderExpectFullLogging() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/error", entity, MyEntity.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test 
	public void useHumanReadableJsonEncoderExpectFullLogging() {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
			MyEntity entity = new MyEntity();
			entity.setName("Entur");
			entity.setSecret("mySecret");

			ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/error", entity, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		}
	}

	@Test
	public void useMachineReadableJsonEncoderExpectFullLogging() throws InterruptedException {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
			MyEntity entity = new MyEntity();
			entity.setName("Entur");
			entity.setSecret("mySecret");

			ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/error", entity, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		}
	}


}