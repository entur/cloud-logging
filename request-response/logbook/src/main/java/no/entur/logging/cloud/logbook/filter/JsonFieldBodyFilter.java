package no.entur.logging.cloud.logbook.filter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.common.MediaTypeQuery;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 
 * Thread-safe filter for JSON fields. 
 *
 */

public class JsonFieldBodyFilter implements BodyFilter {

	private static final Predicate<String> json = MediaTypeQuery.compile("application/json", "application/*+json");

	public static JsonFieldBodyFilter newInstance(String ... fieldNames) {
		return new JsonFieldBodyFilter(Arrays.asList(fieldNames));
	}

	public static JsonFieldBodyFilter newInstance(Collection<String> fieldNames) {
		return new JsonFieldBodyFilter(fieldNames);
	}

	private final Set<String> fields;
	private JsonFactory factory;

	public JsonFieldBodyFilter(Collection<String> fieldNames) {
		this.fields = new HashSet<>(fieldNames); // thread safe for reading
		this.factory = new ObjectMapper().getFactory();
	}

	@Override
	public String filter(String contentType, String body) {
		return json.test(contentType) ? filter(body) : body;
	}

	public String filter(final String body) {
		try (
				final JsonParser parser = factory.createParser(body);
				StringWriter writer = new StringWriter(body.length() * 2);
				JsonGenerator generator = factory.createGenerator(writer);
				) {

			do {
				JsonToken nextToken = parser.nextToken();
				if(nextToken == null) {
					break;
				}

				generator.copyCurrentEvent(parser);
				if(nextToken == JsonToken.FIELD_NAME) {
					if(fields.contains(parser.getCurrentName())) {
						nextToken = parser.nextToken();
						if(nextToken.isScalarValue()) {
							generator.writeString("[filtered by logger]");
						} else if(nextToken.isStructStart()) {
							generator.writeString("[tree filtered by logger]");

							parser.skipChildren(); // skip children
						} else {
							throw new IllegalArgumentException("Unexpected type " + nextToken.name());
						}
					}
				}

			} while(true);

			generator.flush();

			return writer.toString();
		} catch(Exception e) {
			// ignore
		}
		return body;
	}


}
