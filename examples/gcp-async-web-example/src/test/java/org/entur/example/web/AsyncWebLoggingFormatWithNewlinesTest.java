package org.entur.example.web;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class AsyncWebLoggingFormatWithNewlinesTest {

	@LocalServerPort
	private int randomServerPort;
	
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void useHumanReadablePlainEncoderTest() {
		ResponseEntity<MyEntity> response = restTemplate.getForEntity("/api/document/some/newlines", MyEntity.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test 
	public void useHumanReadableJsonEncoderTest() throws InterruptedException {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
			ResponseEntity<MyEntity> response = restTemplate.getForEntity("/api/document/some/newlines", MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}
	}

	@Test
	public void useMachineReadableJsonEncoder() throws InterruptedException {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
			ResponseEntity<MyEntity> response = restTemplate.getForEntity("/api/document/some/newlines", MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}
	}

}