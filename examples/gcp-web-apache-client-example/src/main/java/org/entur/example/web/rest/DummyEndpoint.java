package org.entur.example.web.rest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.CharArrayWriter;
import java.io.IOException;

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