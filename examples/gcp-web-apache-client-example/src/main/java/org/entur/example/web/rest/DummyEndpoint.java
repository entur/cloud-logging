package org.entur.example.web.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 *
 * Simulation of downstream service
 *
 */

@RestController
@RequestMapping("/api/dummy-service")
public class DummyEndpoint {

    private final static Logger logger = LoggerFactory.getLogger(DummyEndpoint.class);

	@PostMapping("/some/method")
	public MyEntity someMessage(@RequestBody MyEntity entity) {
		logger.trace("Hello entity with secret / trace / dummy");
		logger.debug("Hello entity with secret / debug / dummy");
		logger.info("Hello entity with secret / info / dummy");
		logger.warn("Hello entity with secret / warn / dummy");
		logger.error("Hello entity with secret / error / dummy");

		entity.setName("Entur dummy response");
		return entity;
	}


}