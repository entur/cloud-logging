package no.entur.logging.cloud.logbook.logbook.test;

import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Origin;

import java.io.IOException;
import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BodyReplacementHttpResponse implements HttpResponse {

    private final HttpResponse delegate;
    private final String body;

    public BodyReplacementHttpResponse(final HttpResponse delegate, final String body) {
        this.delegate = delegate;
        this.body = body;
    }

    @Override
    public byte[] getBody() throws IOException {
        return body.getBytes(UTF_8);
    }

    @Override
    public String getBodyAsString() throws IOException {
        return body;
    }

	@Override
	public String getProtocolVersion() {
		return delegate.getProtocolVersion();
	}

	@Override
	public Origin getOrigin() {
		return delegate.getOrigin();
	}

	@Override
	public HttpHeaders getHeaders() {
		return delegate.getHeaders();
	}

	@Override
	public String getContentType() {
		return delegate.getContentType();
	}

	@Override
	public Charset getCharset() {
		return delegate.getCharset();
	}

	@Override
	public int getStatus() {
		return delegate.getStatus();
	}
	@Override
	public HttpResponse withBody() throws IOException { return delegate.withBody(); }
	@Override
	public HttpResponse withoutBody() { return delegate.withoutBody(); }

}
