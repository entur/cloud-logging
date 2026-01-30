package org.entur.example.web;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * Note: Expect that logged JSON request body is NOT validated; as there is no exception
 *
 */

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
		"entur.logging.http.ondemand.enabled=true",
		"entur.logging.http.ondemand.failure.http.enabled=false",
		"entur.logging.http.ondemand.failure.logger.enabled=false",
		"entur.logging.http.ondemand.failure.duration.enabled=true",
		"entur.logging.http.ondemand.failure.duration.after=500ms",
})
@AutoConfigureTestRestTemplate
public class OndemandWebLoggingSlowResponseTest {

	@LocalServerPort
	private int randomServerPort;
	
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void useHumanReadablePlainEncoderExpectFullLogging() {
		Long wait = 600L;

		ResponseEntity<MyEntity> response = restTemplate.getForEntity("/api/document/some/slow/method?wait="+wait, MyEntity.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void useHumanReadablePlainEncoderExpectReducedLogging() {
		Long wait = 100L;

		ResponseEntity<MyEntity> response = restTemplate.getForEntity("/api/document/some/slow/method?wait="+wait, MyEntity.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}


	@Test 
	public void useHumanReadableJsonEncoderExpectFullLogging() {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
			Long wait = 600L;

			ResponseEntity<MyEntity> response = restTemplate.getForEntity("/api/document/some/slow/method?wait="+wait, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}
	}


	@Test
	public void useHumanReadableJsonEncoderExpectReducedLogging() {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
			Long wait = 100L;

			ResponseEntity<MyEntity> response = restTemplate.getForEntity("/api/document/some/slow/method?wait="+wait, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}
	}

	@Test
	public void useMachineReadableJsonEncoderExpectFullLogging() {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
			Long wait = 600L;

			ResponseEntity<MyEntity> response = restTemplate.getForEntity("/api/document/some/slow/method?wait="+wait, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}
	}

	@Test
	public void useMachineReadableJsonEncoderExpectReducedLogging() {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
			Long wait = 100L;

			ResponseEntity<MyEntity> response = restTemplate.getForEntity("/api/document/some/slow/method?wait="+wait, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}
	}


}