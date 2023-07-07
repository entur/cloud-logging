package no.entur.logging.cloud.logback.logstash.test.junit.dummy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parent2 implements Runnable {

    public final static Logger logger = LoggerFactory.getLogger(Parent2.class);

	@Override
	public void run() {
		logger.info("Parent 2");
		logger.warn("Parent 2");
		logger.error("Parent 2");
	}
}
