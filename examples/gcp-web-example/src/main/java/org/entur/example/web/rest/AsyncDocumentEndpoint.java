package org.entur.example.web.rest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import no.entur.logging.cloud.appender.scope.LoggingScope;
import no.entur.logging.cloud.spring.ondemand.web.scope.LoggingScopeControls;
import no.entur.logging.cloud.spring.ondemand.web.scope.LoggingScopeThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/async-document")
@Profile("async")
public class AsyncDocumentEndpoint {

    private final static Logger logger = LoggerFactory.getLogger(AsyncDocumentEndpoint.class);

	@Autowired
	private LoggingScopeThreadUtils utils;

	@PostMapping("/some/method")
	public CompletableFuture<MyEntity> someMessage(@RequestBody MyEntity entity) {
		logger.trace("Hello entity with secret / trace");
		logger.debug("Hello entity with secret / debug");
		logger.info("Hello entity with secret / info");
		logger.warn("Hello entity with secret / warn");
		logger.error("Hello entity with secret / error");

		entity.setName("Entur response");
		return CompletableFuture.supplyAsync(utils.withLoggingScope(() -> {
			System.out.println("Complete future on thread " + Thread.currentThread().getName());

			logger.info("Async: This message should be logged / info");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return entity;
		}));
	}

	@PostMapping("/some/error")
	public CompletableFuture<ResponseEntity> errorMethod(@RequestBody MyEntity entity) throws InterruptedException {
		System.out.flush();
		System.out.println("System out before endpoint logging on thread " + Thread.currentThread().getName());

		logger.trace("This message should be ignored / trace");
		logger.debug("This message should be ignored / debug");
		logger.info("This message should be delayed / info");
		logger.warn("This message should be logged / warn");

		Thread.sleep(1000);
		System.out.println("System out after endpoint logging + 1000ms");

		return CompletableFuture.supplyAsync(utils.withLoggingScope( () -> {
			System.out.println("Complete future on thread " + Thread.currentThread().getName());

			logger.info("Async: This message should be delayed / info");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			return new ResponseEntity(HttpStatus.NOT_FOUND);
		}));
	}


}