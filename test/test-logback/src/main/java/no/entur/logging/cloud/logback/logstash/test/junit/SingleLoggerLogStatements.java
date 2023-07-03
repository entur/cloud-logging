package no.entur.logging.cloud.logback.logstash.test.junit;

import java.util.List;

/**
 * 
 * Log statement / event wrapper. Content is backed by parent.
 * 
 */

public class SingleLoggerLogStatements extends LogStatements {
	
	protected final LogStatements parent;
	protected final String loggerName;
	protected int count = 0;

	public SingleLoggerLogStatements(LogStatements parent, String loggerName, long flushDelay) {
		super(flushDelay);
		this.parent = parent;
		this.loggerName = loggerName;
	}

	protected void refresh() {
		parent.refresh();
		
		List<LogStatement> parentStatements = parent.getStatements();

		if(count < parentStatements.size()) {
			for(int i = count; i < parentStatements.size(); i++) {
				LogStatement logStatement = parentStatements.get(i);
				if(logStatement.getLoggerName().startsWith(this.loggerName)) {
					this.statements.add(logStatement);
				}
			}
			count = parentStatements.size();
		}
		
	}

	public LogStatements forLogger(String string) {
		if(!string.startsWith(loggerName)) {
			throw new IllegalArgumentException("Logger must be child of " + loggerName);
		}
		return super.forLogger(string);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		return super.equals(o);
	}
}
