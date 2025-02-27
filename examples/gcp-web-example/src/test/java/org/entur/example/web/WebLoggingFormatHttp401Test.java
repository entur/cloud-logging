package org.entur.example.web;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
	"logbook.secure-filter.enabled=false"
})



public class WebLoggingFormatHttp401Test {

	@LocalServerPort
    private int randomServerPort;
	
	@Test
	public void useHumanReadablePlainEncoderTest() throws Exception {
		URL url = new URL("http://localhost:" + randomServerPort +"/api/secured/endpoint");
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		urlConnection.setRequestProperty("Authorization", "Bearer x.y.z");
		assertEquals(401, urlConnection.getResponseCode());
	}

	@Test 
	public void useHumanReadableJsonEncoderTest() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
			URL url = new URL("http://localhost:" + randomServerPort +"/api/secured/endpoint");
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestProperty("Authorization", "Bearer x.y.z");
			assertEquals(401, urlConnection.getResponseCode());

			Thread.sleep(1000);
		}
	}

	@Test
	public void useMachineReadableJsonEncoder() throws Exception {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
			URL url = new URL("http://localhost:" + randomServerPort +"/api/secured/endpoint");
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestProperty("Authorization", "Bearer x.y.z");
			assertEquals(401, urlConnection.getResponseCode());
		}
	}

}