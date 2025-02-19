package no.entur.logging.cloud.logbook.filter.path.strategy;

import java.io.IOException;
import java.nio.charset.Charset;

import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Origin;
import org.zalando.logbook.attributes.HttpAttributes;

/**
 * A {@link RequestHttpResponse} which remembers its path.
 */

public class DelegateRequestHttpResponse implements RequestHttpResponse {

	private HttpResponse response;
	private HttpRequest request;
	
	public DelegateRequestHttpResponse(HttpRequest request, HttpResponse response, String path) {
		this.request = request;
		this.response = response;
	}

	@Override
	public HttpRequest getRequest() {
		return request;
	}

	public int getStatus() {
		return response.getStatus();
	}

	public HttpResponse withBody() throws IOException {
		return response.withBody();
	}

	public String getProtocolVersion() {
		return response.getProtocolVersion();
	}

	public HttpResponse withoutBody() {
		return response.withoutBody();
	}

	public Origin getOrigin() {
		return response.getOrigin();
	}

	public HttpAttributes getAttributes() {
		return response.getAttributes();
	}

	public HttpHeaders getHeaders() {
		return response.getHeaders();
	}

	public String getContentType() {
		return response.getContentType();
	}

	public String getReasonPhrase() {
		return response.getReasonPhrase();
	}

	public Charset getCharset() {
		return response.getCharset();
	}

	public byte[] getBody() throws IOException {
		return response.getBody();
	}

	public String getBodyAsString() throws IOException {
		return response.getBodyAsString();
	}
	
	
}
