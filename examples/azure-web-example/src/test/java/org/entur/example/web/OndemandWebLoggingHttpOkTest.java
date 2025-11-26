package org.entur.example.web;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)

@TestPropertySource(properties = {"entur.logging.http.ondemand.enabled=true", "entur.logging.http.ondemand.failure.http.statusCode.equalOrHigherThan=400"})
public class OndemandWebLoggingHttpOkTest {

	@LocalServerPort
	private int randomServerPort;

    @Autowired
    private RestTestClient restTestClient;

	@Test
	public void useHumanReadablePlainEncoderExpectReducedLogging() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

        restTestClient.post().uri("/api/document/some/method").contentType(MediaType.APPLICATION_JSON).body(entity).exchange().expectStatus().isOk();
	}

	@Test
	public void useHumanReadableJsonEncoderExpectReducedLogging() throws InterruptedException {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
            restTestClient.post().uri("/api/document/some/method").contentType(MediaType.APPLICATION_JSON).body(entity).exchange().expectStatus().isOk();
		}
	}

	@Test
	public void useMachineReadableJsonEncoderExpectReducedLogging() throws InterruptedException {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
            restTestClient.post().uri("/api/document/some/method").contentType(MediaType.APPLICATION_JSON).body(entity).exchange().expectStatus().isOk();
		}
	}

}