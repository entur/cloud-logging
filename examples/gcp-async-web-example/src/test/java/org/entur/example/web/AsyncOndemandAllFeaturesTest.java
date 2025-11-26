package org.entur.example.web;

import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

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

public class AsyncOndemandAllFeaturesTest {

	@LocalServerPort
	private int randomServerPort;

    @Autowired
    private RestTestClient restTestClient;

	@Test
	public void infoLoggingExpectReducedLogging() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

        restTestClient.post().uri("/api/document/some/method/infoLoggingOnly").contentType(MediaType.APPLICATION_JSON).body(entity).exchange().expectStatus().isOk();
	}

	@Test
	public void errorLoggingExpectFullLogging() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

        restTestClient.post().uri("/api/document/some/method").contentType(MediaType.APPLICATION_JSON).body(entity).exchange().expectStatus().isOk();
	}

	@Test
	public void http404ExpectFullLogging() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

        restTestClient.post().uri("/api/document/some/methodThatDoesNotExist").contentType(MediaType.APPLICATION_JSON).body(entity).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void troubleshootingExpectFullLogging() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

        restTestClient.post().uri("/api/document/some/method/infoLoggingOnly").header("entur-debug-request", "somevalue").contentType(MediaType.APPLICATION_JSON).body(entity).exchange().expectStatus().isOk();
	}

}