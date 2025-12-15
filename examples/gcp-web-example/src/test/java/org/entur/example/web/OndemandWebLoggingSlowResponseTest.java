package org.entur.example.web;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 *
 * Note: Expect that logged JSON request body is NOT validated; as there is no exception
 *
 */

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
		"entur.logging.http.ondemand.enabled=true",
		"entur.logging.http.ondemand.failure.http.enabled=false",
		"entur.logging.http.ondemand.failure.logger.enabled=false",
		"entur.logging.http.ondemand.failure.duration.enabled=true",
		"entur.logging.http.ondemand.failure.duration.after=500ms",
})
public class OndemandWebLoggingSlowResponseTest {

    @Autowired
    private RestTestClient restTestClient;

	@Test
	public void useHumanReadablePlainEncoderExpectFullLogging() {
		Long wait = 600L;

        restTestClient.get().uri("/api/document/some/slow/method?wait="+wait).exchange().expectStatus().isOk();
	}

	@Test
	public void useHumanReadablePlainEncoderExpectReducedLogging() {
		Long wait = 100L;

        restTestClient.get().uri("/api/document/some/slow/method?wait="+wait).exchange().expectStatus().isOk();
	}


	@Test 
	public void useHumanReadableJsonEncoderExpectFullLogging() {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
			Long wait = 600L;

            restTestClient.get().uri("/api/document/some/slow/method?wait="+wait).exchange().expectStatus().isOk();
		}
	}


	@Test
	public void useHumanReadableJsonEncoderExpectReducedLogging() {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
			Long wait = 100L;

            restTestClient.get().uri("/api/document/some/slow/method?wait="+wait).exchange().expectStatus().isOk();
		}
	}

	@Test
	public void useMachineReadableJsonEncoderExpectFullLogging() {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
			Long wait = 600L;

            restTestClient.get().uri("/api/document/some/slow/method?wait="+wait).exchange().expectStatus().isOk();
		}
	}

	@Test
	public void useMachineReadableJsonEncoderExpectReducedLogging() {
		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
			Long wait = 100L;

            restTestClient.get().uri("/api/document/some/slow/method?wait="+wait).exchange().expectStatus().isOk();
		}
	}


}