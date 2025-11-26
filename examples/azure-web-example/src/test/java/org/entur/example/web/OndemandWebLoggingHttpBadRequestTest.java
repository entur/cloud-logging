package org.entur.example.web;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 *
 * Note: Expect that logged JSON request body is validated
 *
 */

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"entur.logging.http.ondemand.enabled=true", "entur.logging.http.ondemand.failure.http.statusCode.equalOrHigherThan=400"})
public class OndemandWebLoggingHttpBadRequestTest {

	@LocalServerPort
	private int randomServerPort;

    @Autowired
    private RestTestClient restTestClient;

	@Test
	public void useHumanReadablePlainEncoderExpectFullLoggingWithoutWellformedBody() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> request = new HttpEntity<>("{invalid json which does not break the log statement syntax}", headers);

        restTestClient.get().uri("/api/document/some/error").exchange().expectStatus().isOk();
	}

	@Test
	public void useHumanReadableJsonEncoderExpectFullLoggingWithoutWellformedBody() {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> request = new HttpEntity<>("{invalid json which does not break the log statement syntax}", headers);

            restTestClient.get().uri("/api/document/some/error").exchange().expectStatus().isOk();
		}
	}

	@Test
	public void useMachineReadableJsonEncoderExpectFullLoggingWithoutWellformedBody() throws InterruptedException {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> request = new HttpEntity<>("{invalid json which does not break the log statement syntax}", headers);

            restTestClient.get().uri("/api/document/some/error").exchange().expectStatus().isOk();
		}
	}

}