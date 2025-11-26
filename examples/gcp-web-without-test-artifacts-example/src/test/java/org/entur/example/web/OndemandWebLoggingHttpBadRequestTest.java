package org.entur.example.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
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
@TestPropertySource(properties = {"entur.logging.http.ondemand.enabled=true", "entur.logging.http.ondemand.failure.http.statusCode.equalOrHigherThan=400"})
public class OndemandWebLoggingHttpBadRequestTest {

    @Autowired
    private RestTestClient restTestClient;

	@Test
	public void useMachineReadableJsonEncoderExpectFullLoggingWithoutWellformedBody() {
        String body = "{invalid json which does not break the log statement syntax}";

        restTestClient.post().uri("/api/document/some/error").contentType(MediaType.APPLICATION_JSON).body(body).exchange().expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
	}

}