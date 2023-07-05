package no.entur.logging.cloud.logbook.logbook.test;

import org.zalando.logbook.HttpHeaders;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.Origin;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BodyReplacementHttpRequest implements HttpRequest {

    private final HttpRequest delegate;
    private final String body;

    public BodyReplacementHttpRequest(final HttpRequest request, final String body) {
        this.delegate = request;
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
	public String getRemote() {
		return delegate.getRemote();
	}

	@Override
	public String getMethod() {
		return delegate.getMethod();
	}

	@Override
	public String getScheme() {
		return delegate.getScheme();
	}

	@Override
	public String getHost() {
		return delegate.getHost();
	}

	@Override
	public Optional<Integer> getPort() {
		return delegate.getPort();
	}

	@Override
	public String getPath() {
		return delegate.getPath();
	}

	@Override
	public String getQuery() {
		return delegate.getQuery();
	}
	@Override
	public HttpRequest withBody() throws IOException { return delegate.withBody(); }
	@Override
	public HttpRequest withoutBody() {
		return delegate.withoutBody();
	}

}
