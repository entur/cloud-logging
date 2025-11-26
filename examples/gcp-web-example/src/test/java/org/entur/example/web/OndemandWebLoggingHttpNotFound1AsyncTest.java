package org.entur.example.web;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * Note: Async does not currently work with on-demand logging.
 *
 */

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("async")
@TestPropertySource(properties = {"entur.logging.http.ondemand.enabled=true", "entur.logging.http.ondemand.failure.http.statusCode.equalOrHigherThan=400", "entur.logging.http.ondemand.failure.logger.level=error", "entur.logging.http.ondemand.failure.level=info"})
public class OndemandWebLoggingHttpNotFound1AsyncTest {

    @Autowired
    private RestTestClient restTestClient;

	@Test
	public void useHumanReadablePlainEncoderExpectFullLogging() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

        restTestClient.post().uri("/api/async-document/some/error").contentType(MediaType.APPLICATION_JSON).body(entity).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void useHumanReadableJsonEncoderExpectFullLogging() {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
			MyEntity entity = new MyEntity();
			entity.setName("Entur");
			entity.setSecret("mySecret");

            restTestClient.post().uri("/api/async-document/some/error").contentType(MediaType.APPLICATION_JSON).body(entity).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
		}
	}

	@Test
	public void useMachineReadableJsonEncoderExpectFullLogging() {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
			MyEntity entity = new MyEntity();
			entity.setName("Entur");
			entity.setSecret("mySecret");

            restTestClient.post().uri("/api/async-document/some/error").contentType(MediaType.APPLICATION_JSON).body(entity).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
		}
	}

}