package no.entur.logging.cloud.logback.logstash.test.junit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.toomuchcoding.jsonassert.JsonAssertion;
import com.toomuchcoding.jsonassert.JsonVerifiable;
import net.logstash.logback.encoder.CompositeJsonEncoder;
import net.logstash.logback.encoder.LogstashEncoder;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;

public class LogStatement {

	protected static final ObjectMapper objectMapper = new ObjectMapper();

	protected final static Comparator<LogStatement> logStatementTimestampComparator = new Comparator<LogStatement>() {
		
		@Override
		public int compare(LogStatement o1, LogStatement o2) {
			return Long.compare(o1.event.getTimeStamp(), o2.event.getTimeStamp());
		}
	};
	
	private CompositeJsonEncoder encoder;
	private ILoggingEvent event;
	private String json;

	public LogStatement(CompositeJsonEncoder encoder, ILoggingEvent event) {
		super();
		this.encoder = encoder;
		this.event = event;
	}
	
	public String getLoggerName() {
		return event.getLoggerName();
	}

	public Level getLogLevel() {
		return event.getLevel();
	}

	public String getJson() {
		if(json == null) {
			json = new String(encoder.encode(event), StandardCharsets.UTF_8);
		}
		return json;
	}
	
	public Map<String, String> getMdc() {
		return event.getMDCPropertyMap();
	}
	
	public String getMessage() {
		return event.getFormattedMessage();
	}
	
	@Override
	public String toString() {
		return getJson();
	}
	
	@Override
	public int hashCode() {
		return getJson().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof LogStatement) {
			LogStatement l = (LogStatement)obj;
			return l.getJson().equals(getJson());
		}
		return super.equals(obj);
	}

	public JsonVerifiable assertThatField(String field) {
		return JsonAssertion.assertThat(getJson()).field(field);
	}

	public JsonVerifiable assertThat() {
		return JsonAssertion.assertThat(getJson());
	}

	public JsonVerifiable assertThatMessage() {
		return JsonAssertion.assertThat(getJson()).field("message");
	}

	public JsonVerifiable assertThatHttpMethod() {
		return JsonAssertion.assertThat(getJson()).field("http").field("method");
	}

	public JsonVerifiable assertThatHttpUri() {
		return JsonAssertion.assertThat(getJson()).field("http").field("uri");
	}

	public JsonVerifiable assertThatHttp() {
		return JsonAssertion.assertThat(getJson()).field("http");
	}
	public JsonVerifiable assertThatHttpBody() {
		return JsonAssertion.assertThat(getJson()).field("http").field("body");
	}

	public JsonVerifiable assertThatHttpStatus() {
		return JsonAssertion.assertThat(getJson()).field("http").field("status");
	}
	
	/**
	 * Return a JsonVerifiable header value. Not that this is an array type.
	 * 
	 * @param name name of http header
	 * @return JsonVerifiable array 
	 */

	public JsonVerifiable assertThatHttpHeader(String name) {
		return JsonAssertion.assertThat(getJson()).field("http").field("headers").array(name);
	}


	public JsonVerifiable assertThatHttpUri(String name) {
		return JsonAssertion.assertThat(getJson()).field("http").field("uri");
	}

	public JsonNode getJsonAsNode() {
		try {
			return objectMapper.readTree(getJson());
		} catch (JacksonException e) {
			throw new RuntimeException("Failed to parse JSON", e);
		}
	}

	public String getJsonPropertyString(String name) {
		try {
			try (JsonParser parser = objectMapper.createParser(getJson())) {
				while (parser.nextToken() != null) {
					if (parser.currentToken() == JsonToken.PROPERTY_NAME) {
						String currentFieldName = parser.currentName();

						if (name.equals(currentFieldName)) {
							if(JsonToken.VALUE_STRING != parser.nextToken()) {
								throw new IllegalArgumentException("Expected a string value for field '" + name + "' but found: " + parser.currentToken());
							}
							return parser.getString();
						}
					}
				}
			}
			return null;
		} catch (JacksonException e) {
			throw new RuntimeException("Failed to parse JSON", e);
		}
	}

}
