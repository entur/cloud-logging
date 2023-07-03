package no.entur.logging.cloud.logback.logstash.test.junit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.encoder.LogstashEncoder;
import no.entur.logging.cloud.api.DevOpsLevel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Log statement / event wrapper.
 * 
 */

public class LogbackLogStatements extends LogStatements {
	
	protected final String[] targets;
	protected final DevOpsLevel level;
	protected final DevOpsLevel severity;
	
	protected final LogbackTestExtension extension;
	
	public LogbackLogStatements(DevOpsLevel level, LogbackTestExtension extension) {
		this(new String[0], level, extension);
	}

	public LogbackLogStatements(String[] targets, DevOpsLevel level, LogbackTestExtension extension) {
		super(extension.getFlushDelay());
		this.targets = Arrays.copyOf(targets, targets.length); // make findbugs happy
		this.severity = level;
		this.extension = extension;
		
		this.level = level;
	}

	/**
	 * In order to support log statements both before and after this object is injected in the unit test, 
	 * make sure it collects the most recent events whenever accessed.
	 */

	protected void refresh() {
		LogstashEncoder encoder = extension.getEncoder();
		List<ILoggingEvent> capture = extension.nextEvents(severity);
		
		for(int i = 0; i < capture.size(); i++) {
			if(targets.length > 0) {
				String loggerName = capture.get(i).getLoggerName();
				for (String target : targets) {
					if(loggerName.startsWith(target)) {
						statements.add(new LogStatement(encoder, capture.get(i)));
						
						break;
					}
				}
			} else {
				statements.add(new LogStatement(encoder, capture.get(i)));
			}
		}
		Collections.sort(statements, LogStatement.logStatementTimestampComparator);
	}

	@Override
	public boolean equals(Object o) {
		return super.equals(o);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
