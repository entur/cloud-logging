package no.entur.logging.cloud.logbook.filter.path.matcher;

import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.Origin;

import java.util.function.Predicate;

/**
 *
 * Matcher which returns the same filter for all origins and sizes.
 *
 */

public class SimplePathFilterMatcher implements PathFilterMatcher {

	protected final Predicate<String> matcher;
	protected final BodyFilter filter;

	public SimplePathFilterMatcher(Predicate<String> matcher, BodyFilter filter) {
		this.matcher = matcher;
		this.filter = filter;
	}

	@Override
	public boolean matches(String path) {
		return matcher.test(path);
	}

	@Override
	public BodyFilter getBodyFilter(Origin origin, int size) {
		return filter;
	}
	
}
