package org.entur.example.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class WebLoggingFormatTest {

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