package no.entur.logging.cloud.logbook.logbook.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpLogFormatter;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Precorrelation;

import java.io.IOException;
import java.io.StringWriter;

/**
 * The payloads have to explicitly be pretty-printed.
 * 
 */

public class PrettyPrintingHttpLogFormatter implements HttpLogFormatter {

	private HttpLogFormatter delegate;
	private JsonFactory factory;

	public PrettyPrintingHttpLogFormatter(HttpLogFormatter delegate) {
		this.delegate = delegate;

		this.factory = new JsonFactory();
	}

	@Override
	public String format(Precorrelation precorrelation, HttpRequest request) throws IOException {

		String contentType = request.getContentType();
		if(ContentType.isJsonMediaType(contentType)) {
			BodyReplacementHttpRequest filtered = new BodyReplacementHttpRequest(request, prettyPrint(new String(request.getBodyAsString())));
			return delegate.format(precorrelation,filtered);
		}
		return delegate.format(precorrelation,request);
	}

	@Override
	public String format(Correlation correlation, HttpResponse reponse) throws IOException {

		String contentType = reponse.getContentType();
		if(ContentType.isJsonMediaType(contentType)) {
			BodyReplacementHttpResponse filtered = new BodyReplacementHttpResponse(reponse, prettyPrint(reponse.getBodyAsString()));
			return delegate.format(correlation,filtered);
		}
		return delegate.format(correlation,reponse);
	}


	public String prettyPrint(String body) {

		if (body != null && body.length() > 0) {
			try (
					JsonParser parser = factory.createParser(body);
					StringWriter writer = new StringWriter(body.length() * 2);
					JsonGenerator generator = factory.createGenerator(writer);
			) {
				generator.useDefaultPrettyPrinter();
				while (parser.nextToken() != null) {
					generator.copyCurrentEvent(parser);
				}
				generator.flush();
				return writer.toString();
			} catch (IOException e) {
				// ignore, keep payload as-is
			}
		}
		return body;
	}

}
