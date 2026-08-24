package org.entur.example.web.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simulation of a downstream service, used to verify that trace IDs are propagated
 * in outgoing HTTP requests and appear in the logs.
 */
@RestController
@RequestMapping("/api/dummy-service")
public class DummyEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(DummyEndpoint.class);

    @PostMapping("/some/method")
    public MyEntity someMessage(@RequestBody MyEntity entity) {
        logger.debug("Downstream service received request / debug");
        logger.info("Downstream service received request / info");
        entity.setName("Entur dummy response");
        return entity;
    }
}
