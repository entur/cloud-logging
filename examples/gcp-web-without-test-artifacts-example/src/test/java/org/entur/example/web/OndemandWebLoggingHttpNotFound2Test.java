package org.entur.example.web;

import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)

/**
 *
 * Note: Expect that logged JSON request body is validated.
 *
 */

@TestPropertySource(properties = {"entur.logging.http.ondemand.enabled=true", "entur.logging.http.ondemand.failure.http.statusCode.equalOrHigherThan=400"})
public class OndemandWebLoggingHttpNotFound2Test {

    @Autowired
    private RestTestClient restTestClient;

	@Test
	public void useMachineReadableJsonEncoderExpectFullLogging() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

        restTestClient.post().uri("/api/document/some/methodThatDoesNotExist").contentType(MediaType.APPLICATION_JSON).body(entity).exchange().expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
	}

}