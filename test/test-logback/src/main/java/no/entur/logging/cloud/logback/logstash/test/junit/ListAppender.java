package no.entur.logging.cloud.logback.logstash.test.junit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import no.entur.logging.cloud.api.DevOpsLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * List appender which keeps track of a read offset.
 *
 */

public class ListAppender extends AppenderBase<ILoggingEvent> {

	protected static final Comparator<ILoggingEvent> loggingEventTimestampComparator = new Comparator<ILoggingEvent>() {
		
		@Override
		public int compare(ILoggingEvent o1, ILoggingEvent o2) {
			return Long.compare(o1.getTimeStamp(), o2.getTimeStamp());
		}
	};

	protected volatile int offset = 0;
    protected List<ILoggingEvent> list = new ArrayList<>(1024);

	@Override
	protected void append(ILoggingEvent e) {
		e.prepareForDeferredProcessing();
		synchronized(list) {
			list.add(e);
		}
	}

	public void clearEvents() {
		synchronized(list) {
			list.clear();
			offset = 0;
		}
	}

	public List<ILoggingEvent> nextEvents(DevOpsLevel level) {
		List<ILoggingEvent> sublist;
		synchronized(list) {
			if(list.size() == offset) {
				sublist = Collections.emptyList();
			} else {
				sublist = list.subList(offset, list.size());
			}
			this.offset = list.size();
			
			if(level != null) {
				sublist = filterLevel(sublist, level);
			}
		}
		return sublist;
	}
	
	protected int size() {
		return list.size();
	}
		
	protected List<ILoggingEvent> filterLevel(List<ILoggingEvent> capture, DevOpsLevel level) {
		int levelInteger = LogbackTestExtension.toLevelInteger(level);

		List<ILoggingEvent> result = capture
					.stream()
					.filter(p ->  p.getLevel().toInt() >= levelInteger)
					.collect(Collectors.toList());
		return result;
	}

}
