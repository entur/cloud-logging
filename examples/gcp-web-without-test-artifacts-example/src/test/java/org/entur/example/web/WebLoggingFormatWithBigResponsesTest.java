package org.entur.example.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public class WebLoggingFormatWithBigResponsesTest {
	
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void useHumanReadablePlainEncoderTest() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/document/some/bigResponse", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

}