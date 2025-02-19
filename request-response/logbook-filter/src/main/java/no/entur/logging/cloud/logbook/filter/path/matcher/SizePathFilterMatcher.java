package no.entur.logging.cloud.logbook.filter.path.matcher;

import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.Origin;

import java.util.function.Predicate;

/**
 *
 * Matcher which selects a filter based on a max size.
 *
 */

public class SizePathFilterMatcher implements PathFilterMatcher  {

	protected final BodyFilter filter;
	protected final BodyFilter maxSizeFilter;

	protected final int maxSize;

	protected final Predicate<String> matcher;

	public SizePathFilterMatcher(Predicate<String> matcher, BodyFilter filter, BodyFilter maxSizeFilter, int maxSize) {
		this.filter = filter;
		this.maxSizeFilter = maxSizeFilter;
		this.maxSize = maxSize;

		this.matcher = matcher;
	}

	@Override
	public boolean matches(String path) {
		return matcher.test(path);
	}

	@Override
	public BodyFilter getBodyFilter(Origin origin, int size) {
		if(size > maxSize) {
			return maxSizeFilter;
		}
		return filter;
	}

}
