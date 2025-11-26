package org.entur.example.web;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 *
 * Note: Expect that logged JSON request body is validated
 *
 */

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
		"entur.logging.http.ondemand.enabled=true",
		"entur.logging.http.ondemand.failure.logger.enabled=false",
})
public class AsyncOndemandWebLoggingHttpBadRequestTest {

	@LocalServerPort
	private int randomServerPort;

    @Autowired
    private RestTestClient restTestClient;

	@Test
	public void useHumanReadablePlainEncoderExpectFullLoggingWithoutWellformedBody() {
        String body = "{invalid json which does not break the log statement syntax}";

        restTestClient.post().uri("/api/document/some/error").contentType(MediaType.APPLICATION_JSON).body(body).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	public void useHumanReadableJsonEncoderExpectFullLoggingWithoutWellformedBody() {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
            String body = "{invalid json which does not break the log statement syntax}";

            restTestClient.post().uri("/api/document/some/error").contentType(MediaType.APPLICATION_JSON).body(body).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
		}
	}

	@Test
	public void useMachineReadableJsonEncoderExpectFullLoggingWithoutWellformedBody() throws InterruptedException {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
            String body = "{invalid json which does not break the log statement syntax}";

            restTestClient.post().uri("/api/document/some/error").contentType(MediaType.APPLICATION_JSON).body(body).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
		}
	}

}