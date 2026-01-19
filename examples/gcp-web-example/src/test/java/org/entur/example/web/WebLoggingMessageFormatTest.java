package org.entur.example.web;

import static org.assertj.core.api.Assertions.assertThat;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "entur.logging.request-response.format.server.message.scheme=false",
        "entur.logging.request-response.format.server.message.host=false",
        "entur.logging.request-response.format.server.message.port=false"
})
@AutoConfigureTestRestTemplate
public class WebLoggingMessageFormatTest {

    @LocalServerPort
    private int randomServerPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void useHumanReadablePlainEncoderTest() {
        MyEntity entity = new MyEntity();
        entity.setName("Entur");
        entity.setSecret("mySecret");

        ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method", entity,
                MyEntity.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void useHumanReadableJsonEncoderTest() throws InterruptedException {
        MyEntity entity = new MyEntity();
        entity.setName("Entur");
        entity.setSecret("mySecret");

        try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
            ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method", entity,
                    MyEntity.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    public void useMachineReadableJsonEncoder() throws InterruptedException {
        MyEntity entity = new MyEntity();
        entity.setName("Entur");
        entity.setSecret("mySecret");

        try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
            ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method", entity,
                    MyEntity.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

}
