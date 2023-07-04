package no.entur.logging.cloud.logback.logstash.test.junit.dummy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parent1 implements Runnable {

    public final static Logger logger = LoggerFactory.getLogger(Parent1.class);

	@Override
	public void run() {
		logger.info("Parent 1");
		logger.warn("Parent 1");
		logger.error("Parent 1");
	}

}
