package no.entur.logging.cloud.logback.logstash.test.junit.dummy.sub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Child1 implements Runnable {

    public final static Logger logger = LoggerFactory.getLogger(Child1.class);

	@Override
	public void run() {
		logger.info("Child 1");
		logger.warn("Child 1");
		logger.error("Child 1");
	}
}
