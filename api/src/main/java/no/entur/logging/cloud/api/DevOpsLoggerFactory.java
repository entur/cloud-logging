package no.entur.logging.cloud.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevOpsLoggerFactory {

	public static DevOpsLogger getLogger(Class<?> clazz) {
		return getLogger(LoggerFactory.getLogger(clazz));
	}

	public static DevOpsLogger getLogger(String name) {
		return getLogger(LoggerFactory.getLogger(name));
	}

	public static DevOpsLogger getLogger(Logger logger) {
		return new DefaultDevOpsLogger(logger);
	}
}
