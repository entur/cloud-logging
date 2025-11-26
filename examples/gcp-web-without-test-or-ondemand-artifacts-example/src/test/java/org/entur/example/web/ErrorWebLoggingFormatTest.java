package org.entur.example.web;

import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ErrorWebLoggingFormatTest {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	public void useMachineReadableJsonEncoder() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

        restTestClient.post().uri("/api/document/some/thrown/error").contentType(MediaType.APPLICATION_JSON).body(entity).exchange().expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

}