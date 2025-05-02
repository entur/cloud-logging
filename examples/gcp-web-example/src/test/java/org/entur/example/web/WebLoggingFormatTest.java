package org.entur.example.web;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import no.entur.logging.cloud.appender.AsyncAppenderBase;
import no.entur.logging.cloud.appender.scope.LoggingScopeAsyncAppender;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControlClosable;
import org.entur.example.web.rest.MyEntity;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Iterator;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class WebLoggingFormatTest {

	@LocalServerPort
    private int randomServerPort;
	
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void useHumanReadablePlainEncoderTest() {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method", entity, MyEntity.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test 
	public void useHumanReadableJsonEncoderTest() throws InterruptedException {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useHumanReadableJsonEncoder()) {
			ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method", entity, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}
	}

	@Test
	public void useMachineReadableJsonEncoder() throws InterruptedException {
		MyEntity entity = new MyEntity();
		entity.setName("Entur");
		entity.setSecret("mySecret");

		try (CompositeConsoleOutputControlClosable c = CompositeConsoleOutputControl.useMachineReadableJsonEncoder()) {
			ResponseEntity<MyEntity> response = restTemplate.postForEntity("/api/document/some/method", entity, MyEntity.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}
	}

	@Test
	public void logFromRemoteExample() throws InterruptedException {
		// one-time init
		UnsynchronizedAppenderBase appender = getAppender();
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

		// per log statement
		// get inputs from remote call
		Level level = Level.INFO;
		Instant instant = Instant.EPOCH;
		String message = "Test me";
		Logger logger = loggerContext.getLogger("my.remote.package");

		// create + write log event
		LoggingEvent logEvent = buildLoggingEvent(logger, instant, "", null, level, message, null, null);
		appender.doAppend(logEvent);
	}

	private LoggingEvent buildLoggingEvent(Logger logger, Instant time, final Marker marker, final Level level, final String msg) {
		return buildLoggingEvent(logger, time, "", marker, level, msg, null, null);
	}

	private LoggingEvent buildLoggingEvent(Logger logger, Instant time, final String localFQCN, final Marker marker, final Level level,
											final String msg, final Object[] params, final Throwable t) {
		LoggingEvent le = new LoggingEvent(localFQCN, logger, level, msg, t, params);
		le.addMarker(marker);
		le.setInstant(time);
		return le;
	}

	public static UnsynchronizedAppenderBase getAppender() {
		Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders();
		while (appenderIterator.hasNext()) {
			Appender<ILoggingEvent> appender = appenderIterator.next();
			if (appender instanceof no.entur.logging.cloud.appender.AsyncAppenderBase a) {
				return a;
			}
		}
		throw new IllegalStateException("Expected async appender");
	}


}