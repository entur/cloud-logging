package no.entur.logging.cloud.logbook.filter.path;

import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.HttpRequest;

/**
 *
 * Interface for returning the proper {@link BodyFilter} for a given request path. <br><br>
 *
 *
 */

public interface RequestFilterCollection {

	BodyFilter getBodyFilter(HttpRequest request, int size);

}
