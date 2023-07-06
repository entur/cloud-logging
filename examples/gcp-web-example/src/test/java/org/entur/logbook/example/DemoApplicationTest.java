package org.entur.logbook.example;

import static org.assertj.core.api.Assertions.assertThat;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import org.entur.logbook.example.rest.MyEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.Closeable;
import java.io.IOException;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class DemoApplicationTest {

	@LocalServerPort
    private int randomServerPort;
	
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void filterPlain() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		ResponseEntity<MyEntity> response = restTemplate.postForEntity("/document/some/method", entity, MyEntity.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test 
	public void useHumanReadableJsonEncoderTest() throws InterruptedException {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
			ResponseEntity<MyEntity> response = restTemplate.postForEntity("/document/some/method", entity, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}

	}

	@Test
	public void useMachineReadableJsonEncoder() throws InterruptedException {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
			ResponseEntity<MyEntity> response = restTemplate.postForEntity("/document/some/method", entity, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}

	}
	
	
}