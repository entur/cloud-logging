package no.entur.logging.cloud.logbook.filter.path.matcher;

import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.Origin;

import java.util.function.Predicate;

/**
 *
 * Matcher which selects a filter based on origin and max size.
 *
 */


public class SizeOriginPathFilterMatcher implements PathFilterMatcher  {

	protected final BodyFilter remoteFilter;
	protected final BodyFilter remoteMaxSizeFilter;

	protected final BodyFilter localFilter;
	protected final BodyFilter localMaxSizeFilter;

	protected final int maxSize;

	protected final Predicate<String> matcher;

	public SizeOriginPathFilterMatcher(Predicate<String> matcher, BodyFilter remoteFilter, BodyFilter remoteMaxSizeFilter, BodyFilter localFilter, BodyFilter localMaxSizeFilter, int maxSize) {
		this.remoteFilter = remoteFilter;
		this.remoteMaxSizeFilter = remoteMaxSizeFilter;
		this.localFilter = localFilter;
		this.localMaxSizeFilter = localMaxSizeFilter;
		this.maxSize = maxSize;

		this.matcher = matcher;
	}

	@Override
	public boolean matches(String path) {
		return matcher.test(path);
	}

	@Override
	public BodyFilter getBodyFilter(Origin origin, int size) {
		if(origin == Origin.REMOTE) {
			if(size > maxSize) {
				return remoteMaxSizeFilter;
			}
			return remoteFilter;
		}
		if(size > maxSize) {
			return localMaxSizeFilter;
		}
		return localFilter;
	}

}
