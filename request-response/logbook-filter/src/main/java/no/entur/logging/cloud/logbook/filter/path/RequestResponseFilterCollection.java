package no.entur.logging.cloud.logbook.filter.path;

import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;

/**
 *
 * Interface for returning the proper {@link BodyFilter} for a given response path. <br><br>
 *
 */

public interface RequestResponseFilterCollection {

	BodyFilter getBodyFilter(HttpRequest request, HttpResponse httpResponse, int size);

}
