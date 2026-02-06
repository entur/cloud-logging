package org.entur.example.web;

import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)

@TestPropertySource(properties = {
		"entur.logging.request-response.logger.name=org.entur.offers.http",
		"entur.logging.request-response.logger.level=info",
		"entur.logging.http.ondemand.enabled=true",
		"entur.logging.http.ondemand.success.level=warn",
		"entur.logging.http.ondemand.failure.level=info",
		"entur.logging.http.ondemand.failure.http.statusCode.equalOrHigherThan=404",
		"entur.logging.http.ondemand.failure.logger.level=error",
		"entur.logging.http.ondemand.troubleshoot.level=debug",
		"entur.logging.http.ondemand.troubleshoot.http.headers[0].name=entur-debug-request",
		"entur.logging.http.ondemand.flush-mode=LAZY",
})
@AutoConfigureTestRestTemplate

public class AsyncOndemandAllFeaturesTest {

	@LocalServerPort
	private int randomServerPort;
	
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void infoLoggingExpectReducedLogging() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method/infoLoggingOnly", entity, MyEntity.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void errorLoggingExpectFullLogging() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method", entity, MyEntity.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void http404ExpectFullLogging() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/methodThatDoesNotExist", entity, MyEntity.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void troubleshootingExpectFullLogging() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("entur-debug-request", "somevalue");

		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		HttpEntity<MyEntity> request = new HttpEntity<>(entity, headers);
		ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method/infoLoggingOnly", request, MyEntity.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

}