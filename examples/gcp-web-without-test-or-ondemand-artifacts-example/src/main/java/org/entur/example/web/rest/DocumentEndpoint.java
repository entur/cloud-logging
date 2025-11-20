package org.entur.example.web.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/document")
public class DocumentEndpoint {

    private final static Logger logger = LoggerFactory.getLogger(DocumentEndpoint.class);

	@PostMapping("/some/method")
	public MyEntity someMessage(@RequestBody MyEntity entity) {
		logger.trace("Hello entity with secret / trace");
		logger.debug("Hello entity with secret / debug");
		logger.info("Hello entity with secret / info");
		logger.warn("Hello entity with secret / warn");
		logger.error("Hello entity with secret / error");

		entity.setName("Entur response");
		return entity;
	}

	@PostMapping("/some/error")
	public ResponseEntity errorMethod(@RequestBody MyEntity entity) throws InterruptedException {
		System.out.flush();
		System.out.println("System out before endpoint logging");

		logger.trace("This message should be ignored / trace");
		logger.debug("This message should be ignored / debug");
		logger.info("This message should be delayed / info");
		logger.warn("This message should be logged / warn");
		logger.error("This message should be logged / error");

		Thread.sleep(1000);
		System.out.println("System out after endpoint logging + 1000ms");


		return new ResponseEntity(HttpStatus.NOT_FOUND);
	}

    @PostMapping("/some/thrown/error")
    public ResponseEntity throwErrorMethod(@RequestBody MyEntity entity) throws InterruptedException {
        System.out.flush();
        System.out.println("System out before endpoint logging");

        logger.trace("This message should be ignored / trace");
        logger.debug("This message should be ignored / debug");
        logger.info("This message should be delayed / info");
        logger.warn("This message should be logged / warn");
        logger.error("This message should be logged / error");

        Thread.sleep(1000);
        System.out.println("System out after endpoint logging + 1000ms");

        throw new NullPointerException("MOCK");
    }


}