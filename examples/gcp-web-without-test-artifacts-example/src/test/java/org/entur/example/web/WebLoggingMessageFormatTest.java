package org.entur.example.web;

import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "entur.logging.request-response.format.server.message.scheme=false",
        "entur.logging.request-response.format.server.message.host=false",
        "entur.logging.request-response.format.server.message.port=false"
})
public class WebLoggingMessageFormatTest {

    @LocalServerPort
    private int randomServerPort;

    @Autowired
    private RestTestClient restTestClient;

    @Test
    public void useMachineReadableJsonEncoder() {
        MyEntity entity = new MyEntity();
        entity.setName("Entur");
        entity.setSecret("mySecret");

        restTestClient.post().uri("/api/document/some/method").contentType(MediaType.APPLICATION_JSON).body(entity).exchange().expectStatus().isOk();
    }

}
