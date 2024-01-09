package no.entur.logging.cloud.gcp.spring.test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.github.skjolber.decorators.factory.ConfigurableSyntaxHighlighterFactory;
import com.github.skjolber.jackson.jsh.AnsiSyntaxHighlight;
import com.github.skjolber.jackson.jsh.DefaultSyntaxHighlighter;
import com.github.skjolber.jackson.jsh.SyntaxHighlighter;

public class LogSeveritySyntaxHighlighterFactory extends ConfigurableSyntaxHighlighterFactory {

	public static class Severity {
		
		private String defaultValue = LogSeveritySyntaxHighlighter.DEFAULT;
		private String debug = LogSeveritySyntaxHighlighter.DEBUG;
		private String info = LogSeveritySyntaxHighlighter.INFO;
		private String warning = LogSeveritySyntaxHighlighter.WARN;
		private String error = LogSeveritySyntaxHighlighter.ERROR;

		private String critical = LogSeveritySyntaxHighlighter.CRITICAL;

		private String alert = LogSeveritySyntaxHighlighter.ALERT;

		private String emergency = LogSeveritySyntaxHighlighter.EMERGENCY;

		public static final String CRITICAL = AnsiSyntaxHighlight.build(AnsiSyntaxHighlight.BACKGROUND_RED, AnsiSyntaxHighlight.WHITE);

		public static final String ALERT = AnsiSyntaxHighlight.build(AnsiSyntaxHighlight.BACKGROUND_RED, AnsiSyntaxHighlight.WHITE);

		public static final String EMERGENCY = AnsiSyntaxHighlight.build(AnsiSyntaxHighlight.BACKGROUND_RED, AnsiSyntaxHighlight.WHITE);

		public void setDefault(String value) {
			this.defaultValue = AnsiSyntaxHighlight.build(parseColors(value));
		}
		
		public void setDebug(String value) {
			this.debug = AnsiSyntaxHighlight.build(parseColors(value));
		}
		
		public void setInfo(String value) {
			this.info = AnsiSyntaxHighlight.build(parseColors(value));
		}
		
		public void setWarning(String value) {
			this.warning = AnsiSyntaxHighlight.build(parseColors(value));
		}
		
		public void setError(String value) {
			this.error = AnsiSyntaxHighlight.build(parseColors(value));
		}

		public void setCritical(String value) {
			this.critical = AnsiSyntaxHighlight.build(parseColors(value));
		}

		public void setAlert(String value) {
			this.alert = AnsiSyntaxHighlight.build(parseColors(value));
		}

		public void setEmergency(String value) {
			this.emergency = AnsiSyntaxHighlight.build(parseColors(value));
		}

	}
	
	protected SyntaxHighlighter cachedSyntaxHighlighter;
	
	protected Severity severity = new Severity();
	protected String message = LogSeveritySyntaxHighlighter.MESSAGE;

	protected String loggerName = LogSeveritySyntaxHighlighter.LOGGER_NAME;
	
	public LogSeveritySyntaxHighlighterFactory() {
		builder = DefaultSyntaxHighlighter.newBuilder();
	}
	
	@Override
	public SyntaxHighlighter createSyntaxHighlighter(JsonGenerator generator) {
		if(cachedSyntaxHighlighter == null) {
			cachedSyntaxHighlighter = super.createSyntaxHighlighter(generator);
		}
		return new LogSeveritySyntaxHighlighter(cachedSyntaxHighlighter, severity.defaultValue, severity.debug, severity.info, severity.warning, severity.error, severity.critical, severity.alert, severity.emergency, loggerName, message);
	}
	
	public void setSeverity(Severity severity) {
		this.severity = severity;
	}
	
	public void setMessage(String value) {
		this.message = AnsiSyntaxHighlight.build(parseColors(value));
	}

	public void setLoggerName(String value) {
		this.loggerName = AnsiSyntaxHighlight.build(parseColors(value));
	}
}
