package no.entur.logging.cloud.logback.logstash.test.junit.dummy.sub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Child2 implements Runnable {

    public final static Logger logger = LoggerFactory.getLogger(Child2.class);

	@Override
	public void run() {
		logger.info("Child 2");
		logger.warn("Child 2");
		logger.error("Child 2");
	}

}
