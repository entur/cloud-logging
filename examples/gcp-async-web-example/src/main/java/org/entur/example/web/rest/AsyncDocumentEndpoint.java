package org.entur.example.web.rest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import no.entur.logging.cloud.spring.ondemand.web.scope.LoggingScopeThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/document")
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

		entity.setName("Entur response");
		return CompletableFuture.supplyAsync(utils.with(() -> {
			System.out.println("Async: System out on thread " + Thread.currentThread().getName());

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            logger.trace("Async: Hello entity with secret / trace");
			logger.debug("Async: Hello entity with secret / debug");
			logger.info("Async: Hello entity with secret / info");
			logger.warn("Async: Hello entity with secret / warn");
			logger.error("Async: Hello entity with secret / error");

			return entity;
		}));
	}

	@PostMapping("/some/error")
	public CompletableFuture<ResponseEntity> errorMethod(@RequestBody MyEntity entity) throws InterruptedException {
		System.out.flush();
		System.out.println("System out before endpoint logging");

		logger.trace("This message should be ignored / trace");
		logger.debug("This message should be ignored / debug");
		logger.info("This message should be delayed / info");
		logger.warn("This message should be logged / warn");
		logger.error("This message should be logged / error");

		Thread.sleep(1000);
		System.out.println("System out after endpoint logging + 1000ms");


		return CompletableFuture.supplyAsync(utils.with(() -> {
			System.out.println("Async: System out on thread " + Thread.currentThread().getName());

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			logger.trace("Async: This message should be ignored / trace");
			logger.debug("Async: This message should be ignored / debug");
			logger.info("Async: This message should be delayed / info");
			logger.warn("Async: This message should be logged / warn");
			logger.error("Async: This message should be logged / error");

			return new ResponseEntity(HttpStatus.NOT_FOUND);
		}));
	}

	@GetMapping(value = "/some/newlines", produces = "application/json")
	public CompletableFuture<ResponseEntity<String>> age() {
		String json = "{\n}\n";

		return CompletableFuture.supplyAsync(utils.with(() -> new ResponseEntity<>(json, HttpStatus.OK)));
	}

	@GetMapping(value = "/some/bigResponse", produces = "application/json")
	public CompletableFuture<ResponseEntity<String>> bigResponse() throws IOException {
		JsonFactory factory = new JsonFactory();

		CharArrayWriter writer = new CharArrayWriter();

		JsonGenerator generator = factory.createGenerator(writer);

		generator.writeStartObject();
		generator.writeStringField("start", "here");
		generator.writeStringField("longValue", generateLongString(64*1024));
		generator.writeStringField("end", "here");
		generator.writeEndObject();

		generator.flush();

		return CompletableFuture.supplyAsync(utils.with(() -> new ResponseEntity<>(writer.toString(), HttpStatus.OK)));
	}

	private String generateLongString(int length) {
		StringBuilder builder = new StringBuilder(length);

		int mod = 'z' - 'a';

		for(int i = 0; i < length; i++) {
			char c = (char) ('a' + i % mod);
			builder.append(c);
		}
		return builder.toString();
	}

	@PostMapping("/some/method/infoLoggingOnly")
	public CompletableFuture<MyEntity> infoLoggingOnly(@RequestBody MyEntity entity) {
		logger.info("Hello entity with secret / info");

		entity.setName("Entur response");
		return CompletableFuture.supplyAsync(utils.with(() -> {
			System.out.println("Async: System out on thread " + Thread.currentThread().getName());

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}


			logger.info("Async: Hello entity with secret / info");

			return entity;
		}));
	}


}