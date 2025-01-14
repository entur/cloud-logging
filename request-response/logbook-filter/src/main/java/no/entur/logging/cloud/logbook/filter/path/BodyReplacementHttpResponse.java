package no.entur.logging.cloud.logbook.filter.path;
import org.zalando.logbook.ForwardingHttpResponse;
import org.zalando.logbook.HttpResponse;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.charset.StandardCharsets;

public class BodyReplacementHttpResponse implements ForwardingHttpResponse, HttpResponse {

    private final HttpResponse response;
    private final String replacement;

    public BodyReplacementHttpResponse(HttpResponse response, String replacement) {
		super();
		this.response = response;
		this.replacement = replacement;
	}

	@Override
    public HttpResponse delegate() {
        return response;
    }

    @Override
    public HttpResponse withBody() {
        return withoutBody();
    }

    @Override
    public HttpResponse withoutBody() {
        return new BodyReplacementHttpResponse(response.withoutBody(), replacement);
    }

    @Override
    public byte[] getBody() {
        return replacement.getBytes(UTF_8);
    }

    @Override
    public String getBodyAsString() {
        return replacement;
    }

}