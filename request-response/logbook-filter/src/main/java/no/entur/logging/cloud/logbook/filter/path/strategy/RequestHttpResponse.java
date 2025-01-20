package no.entur.logging.cloud.logbook.filter.path.strategy;

import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;

public interface RequestHttpResponse extends HttpResponse {

	HttpRequest getRequest();
	
}
