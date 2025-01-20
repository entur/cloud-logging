package no.entur.logging.cloud.logbook.filter.path;

import java.nio.charset.StandardCharsets;

import org.zalando.logbook.ForwardingHttpRequest;
import org.zalando.logbook.HttpRequest;

public class BodyReplacementHttpRequest implements ForwardingHttpRequest {

    private final HttpRequest request;
    private final String replacement;
    
    public BodyReplacementHttpRequest(HttpRequest request, String replacement) {
		super();
		this.request = request;
		this.replacement = replacement;
	}

	@Override
    public HttpRequest delegate() {
        return request;
    }

    @Override
    public HttpRequest withBody() {
        return withoutBody();
    }

    @Override
    public HttpRequest withoutBody() {
        return new BodyReplacementHttpRequest(request.withoutBody(), replacement);
    }

    @Override
    public byte[] getBody() {
        return replacement.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getBodyAsString() {
        return replacement;
    }

}