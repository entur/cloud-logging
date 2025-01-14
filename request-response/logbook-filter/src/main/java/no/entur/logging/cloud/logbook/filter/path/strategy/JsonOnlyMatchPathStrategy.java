package no.entur.logging.cloud.logbook.filter.path.strategy;

import org.zalando.logbook.ContentType;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;

import java.util.List;
import java.util.function.Predicate;

public class JsonOnlyMatchPathStrategy extends MatchPathStrategy {

    public JsonOnlyMatchPathStrategy(List<Predicate<String>> excludeRequests, List<Predicate<String>> excludeResponses) {
        super(excludeRequests, excludeResponses);
    }

    @Override
    protected boolean includeRequest(HttpRequest request) {
        String contentType = request.getContentType();
        if(contentType != null && !ContentType.isJsonMediaType(contentType)) {
            return false;
        }
        return super.includeRequest(request);
    }

    @Override
    protected boolean includeResponse(HttpRequest request, HttpResponse response) {
        String contentType = response.getContentType();
        if(contentType != null && !ContentType.isJsonMediaType(contentType)) {
            return false;
        }
        return super.includeResponse(request, response);
    }
}
