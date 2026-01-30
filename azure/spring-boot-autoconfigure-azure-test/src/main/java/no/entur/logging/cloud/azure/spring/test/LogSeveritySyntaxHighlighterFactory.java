package no.entur.logging.cloud.azure.spring.test;

import org.entur.jackson.tools.jsh.AnsiSyntaxHighlight;
import org.entur.jackson.tools.jsh.DefaultSyntaxHighlighter;
import org.entur.jackson.tools.jsh.SyntaxHighlighter;
import tools.jackson.core.JsonGenerator;
import org.entur.decorators.factory.ConfigurableSyntaxHighlighterFactory;

public class LogSeveritySyntaxHighlighterFactory extends ConfigurableSyntaxHighlighterFactory {

	public static class Severity {
		
		private String defaultValue = LogSeveritySyntaxHighlighter.DEFAULT;
		private String debug = LogSeveritySyntaxHighlighter.DEBUG;
		private String info = LogSeveritySyntaxHighlighter.INFO;
		private String warning = LogSeveritySyntaxHighlighter.WARN;
		private String error = LogSeveritySyntaxHighlighter.ERROR;

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

	}
	
	protected SyntaxHighlighter cachedSyntaxHighlighter;
	
	protected Severity severity = new Severity();
	protected String message = LogSeveritySyntaxHighlighter.MESSAGE;

	protected String loggerName = LogSeveritySyntaxHighlighter.LOGGER_NAME;
	
	public LogSeveritySyntaxHighlighterFactory() {
		builder = DefaultSyntaxHighlighter.newBuilder();
	}
	
	@Override
	public SyntaxHighlighter createSyntaxHighlighter() {
		if(cachedSyntaxHighlighter == null) {
			cachedSyntaxHighlighter = super.createSyntaxHighlighter();
		}
		return new LogSeveritySyntaxHighlighter(cachedSyntaxHighlighter, severity.defaultValue, severity.debug, severity.info, severity.warning, severity.error, loggerName, message);
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
