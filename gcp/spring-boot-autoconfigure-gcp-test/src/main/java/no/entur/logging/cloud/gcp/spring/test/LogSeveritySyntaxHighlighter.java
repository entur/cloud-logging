package no.entur.logging.cloud.gcp.spring.test;

import org.entur.decorators.syntaxhighlight.DelegateSyntaxHighlighter;
import org.entur.jackson.jsh.AnsiSyntaxHighlight;
import org.entur.jackson.jsh.SyntaxHighlighter;

public class LogSeveritySyntaxHighlighter extends DelegateSyntaxHighlighter {

	public static final String DEFAULT = AnsiSyntaxHighlight.build(AnsiSyntaxHighlight.GREEN);
	public static final String DEBUG = AnsiSyntaxHighlight.build(AnsiSyntaxHighlight.GREEN);
	public static final String INFO = AnsiSyntaxHighlight.build(AnsiSyntaxHighlight.GREEN);
	public static final String WARN = AnsiSyntaxHighlight.build(AnsiSyntaxHighlight.YELLOW);
	public static final String ERROR = AnsiSyntaxHighlight.build(AnsiSyntaxHighlight.RED);

	public static final String CRITICAL = AnsiSyntaxHighlight.build(AnsiSyntaxHighlight.HIGH_INTENSITY, AnsiSyntaxHighlight.RED);

	public static final String ALERT = AnsiSyntaxHighlight.build(AnsiSyntaxHighlight.HIGH_INTENSITY, AnsiSyntaxHighlight.RED);

	public static final String EMERGENCY = AnsiSyntaxHighlight.build(AnsiSyntaxHighlight.HIGH_INTENSITY, AnsiSyntaxHighlight.RED);

	public static final String LOGGER_NAME = AnsiSyntaxHighlight.build(AnsiSyntaxHighlight.CYAN);

	public static final String MESSAGE = AnsiSyntaxHighlight.build(AnsiSyntaxHighlight.HIGH_INTENSITY, AnsiSyntaxHighlight.BLACK);

	protected boolean severityLevelField;
	protected boolean loggerNameField;

	protected String defaultSeverity;
	protected String debug;
	protected String info;
	protected String warn;
	protected String error;

	protected String critical;

	protected String alert;

	protected String emergency;

	protected boolean messageField;
	protected String message;

	protected String loggerName;

	public LogSeveritySyntaxHighlighter(SyntaxHighlighter delegate, String defaultSeverity, String debug, String info, String warn, String error, String critical, String alert, String emergency, String loggerName, String message) {
		super(delegate);
		
		this.defaultSeverity = defaultSeverity;
		this.debug = debug;
		this.info = info;
		this.warn = warn;
		this.error = error;
		this.critical = critical;
		this.alert = alert;
		this.emergency = emergency;

		this.loggerName = loggerName;
		this.message = message;
	}

	public LogSeveritySyntaxHighlighter(SyntaxHighlighter delegate) {
		this(delegate, DEFAULT, DEBUG, INFO, WARN, ERROR, CRITICAL, ALERT, EMERGENCY, LOGGER_NAME, MESSAGE);
	}

	@Override
	public String forFieldName(String value) {
		this.severityLevelField = "severity".equals(value);
		this.loggerNameField = "logger_name".equals(value);
		this.messageField = "message".equals(value);
		
		return super.forFieldName(value);
	}
	
	@Override
	public String forString(String string) {
		if(severityLevelField) {
			severityLevelField = false;
			
			if(string != null) {
				if(string.equals("DEFAULT")) {
					return defaultSeverity;
				} else if(string.equals("DEBUG")) {
					return debug;
				} else if(string.equals("INFO")) {
					return info;
				} else if(string.equals("WARNING")) {
					return warn;
				} else if(string.equals("ERROR")) {
					return error;
				} else if(string.equals("CRITICAL")) {
					return critical;
				} else if(string.equals("ALERT")) {
					return alert;
				} else if(string.equals("EMERGENCY")) {
					return emergency;
				}
			}
		} else if(messageField) {
			messageField = false;
			
			return message;
		} else if(loggerNameField) {
			loggerNameField = false;

			return loggerName;
		}
		return super.forString(string);
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public String getWarn() {
		return warn;
	}

	public void setWarn(String warn) {
		this.warn = warn;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

	public void setAlert(String alert) {
		this.alert = alert;
	}

	public void setCritical(String critical) {
		this.critical = critical;
	}

	public void setEmergency(String emergency) {
		this.emergency = emergency;
	}

	public String getAlert() {
		return alert;
	}

	public String getCritical() {
		return critical;
	}

	public String getEmergency() {
		return emergency;
	}
}