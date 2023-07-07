package no.entur.logging.cloud.logback.logstash.test.junit;

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.slf4j.LoggerFactory;

/**
 * Tests can begin before Logback has completed setup.
 * 
 * https://billykorando.com/category/automated-testing/
 *
 */

public class LogbackInitializerExtension implements BeforeAllCallback, TestInstancePostProcessor {

	private final static long timeout = 10 * 1000L;

	public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
		pauseTillLogbackReady();
	}
	
	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		pauseTillLogbackReady();
	}

	protected void pauseTillLogbackReady() {
		// use a deadline so that we're not stuck forever
		long deadline = System.currentTimeMillis() + timeout;
		while(!isLogbackReady()) {
			if(System.currentTimeMillis() > deadline) {
				throw new IllegalStateException("Expected Logback logging factory initialized within timeout " + timeout + "ms");
			}
			Thread.yield();
		}
	}

	protected boolean isLogbackReady() {
		return LoggerFactory.getILoggerFactory() instanceof LoggerContext;
	}
}