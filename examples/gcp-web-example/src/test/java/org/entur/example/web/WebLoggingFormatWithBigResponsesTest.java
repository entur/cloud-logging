package org.entur.example.web;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class WebLoggingFormatWithBigResponsesTest {

    @Autowired
    private RestTestClient restTestClient;

	@Test
	public void useHumanReadablePlainEncoderTest() {
        restTestClient.get().uri("/api/document/some/bigResponse").exchange().expectStatus().isOk();
	}

	@Test 
	public void useHumanReadableJsonEncoderTest() throws InterruptedException {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
            restTestClient.get().uri("/api/document/some/bigResponse").exchange().expectStatus().isOk();
		}
	}

	@Test
	public void useMachineReadableJsonEncoder() throws InterruptedException {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
            restTestClient.get().uri("/api/document/some/bigResponse").exchange().expectStatus().isOk();
		}
	}

}