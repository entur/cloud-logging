package org.entur.example.web;

import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)

@TestPropertySource(properties = {
		"entur.logging.http.ondemand.enabled=true",
		"entur.logging.http.ondemand.failure.http.statusCode.equalOrHigherThan=400",
		"entur.logging.http.ondemand.troubleshoot.http.headers[0].name=X-DEBUG"
})
public class OndemandWebLoggingHttpOkTroubleshootingTest {

	@LocalServerPort
	private int randomServerPort;
	
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void useMachineReadableJsonEncoderExpectReducedLogging() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method", entity, MyEntity.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}


	@Test
	public void useMachineReadableJsonEncoderExpectDebugLogging() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-DEBUG", "somevalue");

		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		HttpEntity<MyEntity> request = new HttpEntity<>(entity, headers);

		ResponseEntity<MyEntity> response = restTemplate.exchange("/api/document/some/method", HttpMethod.POST, request, MyEntity.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}


}