package org.entur.example.web;

import no.entur.logging.cloud.gcp.logback.logstash.StackdriverMicrometerTraceMdcJsonProvider;
import no.entur.logging.cloud.logback.logstash.test.junit.CaptureLogStatements;
import no.entur.logging.cloud.logback.logstash.test.junit.LogStatement;
import no.entur.logging.cloud.logback.logstash.test.junit.LogStatements;
import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;

/**
 * Verifies that the trace ID is propagated from the caller (DocumentEndpoint) to the downstream
 * service (DummyEndpoint) when using a RestTemplate instrumented by the OpenTelemetry agent.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@CaptureLogStatements({"org.entur"})
public class TraceIdPropagationTest {

    private static final String DOCUMENT_ENDPOINT_LOGGER = "org.entur.example.web.rest.DocumentEndpoint";
    private static final String DUMMY_ENDPOINT_LOGGER = "org.entur.example.web.rest.DummyEndpoint";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void traceIdIsPropagatedToDownstreamService(LogStatements statements) {
        MyEntity entity = new MyEntity();
        entity.setName("Entur");
        entity.setSecret("mySecret");

        ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/downstream", entity, MyEntity.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Allow log events to flush
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<LogStatement> documentLogs = statements.stream()
                .filter(s -> DOCUMENT_ENDPOINT_LOGGER.equals(s.getLoggerName()))
                .collect(Collectors.toList());

        List<LogStatement> dummyLogs = statements.stream()
                .filter(s -> DUMMY_ENDPOINT_LOGGER.equals(s.getLoggerName()))
                .collect(Collectors.toList());

        Assertions.assertFalse(documentLogs.isEmpty(), "Expected log statements from DocumentEndpoint");
        Assertions.assertFalse(dummyLogs.isEmpty(), "Expected log statements from DummyEndpoint");

        // Collect all distinct trace IDs from each endpoint
        Set<String> documentTraceIds = documentLogs.stream()
                .map(s -> s.getJsonPropertyString(StackdriverMicrometerTraceMdcJsonProvider.GCP_TRACE_KEY))
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());

        Set<String> dummyTraceIds = dummyLogs.stream()
                .map(s -> s.getJsonPropertyString(StackdriverMicrometerTraceMdcJsonProvider.GCP_TRACE_KEY))
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());

        Assertions.assertFalse(documentTraceIds.isEmpty(), "DocumentEndpoint logs must contain a trace ID");
        Assertions.assertFalse(dummyTraceIds.isEmpty(), "DummyEndpoint logs must contain a trace ID");

        // The downstream call must use the same trace ID as the caller
        assertThat(dummyTraceIds).containsAtLeastElementsIn(documentTraceIds);
    }
}
