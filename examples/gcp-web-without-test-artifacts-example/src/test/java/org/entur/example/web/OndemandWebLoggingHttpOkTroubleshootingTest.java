package org.entur.example.web;

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

@TestPropertySource(properties = {
		"entur.logging.http.ondemand.enabled=true",
		"entur.logging.http.ondemand.failure.http.statusCode.equalOrHigherThan=400",
		"entur.logging.http.ondemand.troubleshoot.http.headers[0].name=X-DEBUG"
})
public class OndemandWebLoggingHttpOkTroubleshootingTest {

	@LocalServerPort
	private int randomServerPort;

    @Autowired
    private RestTestClient restTestClient;

	@Test
	public void useMachineReadableJsonEncoderExpectReducedLogging() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

        restTestClient.post().uri("/api/document/some/method").contentType(MediaType.APPLICATION_JSON).body(entity).exchange().expectStatus().isOk();
	}


	@Test
	public void useMachineReadableJsonEncoderExpectDebugLogging() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

        restTestClient.post().uri("/api/document/some/method").contentType(MediaType.APPLICATION_JSON).body(entity).exchange().expectStatus().isOk();
	}


}