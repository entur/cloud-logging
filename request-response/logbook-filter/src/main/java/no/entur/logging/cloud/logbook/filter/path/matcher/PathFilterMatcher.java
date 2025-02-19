package no.entur.logging.cloud.logbook.filter.path.matcher;

import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.Origin;

public interface PathFilterMatcher {

	boolean matches(String path);
	
	BodyFilter getBodyFilter(Origin origin, int size);
	
}
