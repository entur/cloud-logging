package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collapses repeated WARN (and above) events before they reach the attached appenders: the first occurrence is forwarded,
 * later identical ones are dropped and counted, and a single line
 * <code>repeated N more times (first #id, last #id): message</code> is forwarded when the entry is evicted or the appender stops.
 * <p>
 * Events are identical if level, logger and root cause (or, without an exception, the message) are equal.
 * Memory is bounded: at most {@link #setMaxEntries(int) maxEntries} small entries are kept (least recently used evicted,
 * summary written first, so no counts are lost). Lower levels are always forwarded.
 * Disable with <code>-Dentur.logging.ai.dedupe=false</code>.
 */
public class DeduplicatingAppender extends AppenderBase<ILoggingEvent> implements AppenderAttachable<ILoggingEvent> {

    private static final int MAX_SUMMARY_MESSAGE = 200;

    private static class Entry {
        final Level level;
        final String loggerName;
        final String message;
        final String firstId;
        String lastId;
        long suppressed;

        Entry(ILoggingEvent event, String id) {
            this.level = event.getLevel();
            this.loggerName = event.getLoggerName();
            this.message = abbreviate(describe(event));
            this.firstId = id;
            this.lastId = id;
        }
    }

    private final AppenderAttachableImpl<ILoggingEvent> appenders = new AppenderAttachableImpl<>();

    private int maxEntries = 500;

    private Map<String, Entry> entries;

    private boolean enabled;

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    @Override
    public void start() {
        enabled = AiLoggingControl.isDedupeEnabled();
        entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
                if (size() > maxEntries) {
                    writeSummary(eldest.getValue());
                    return true;
                }
                return false;
            }
        };
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!enabled || !event.getLevel().isGreaterOrEqual(Level.WARN)) {
            appenders.appendLoopOnAppenders(event);
            return;
        }
        String key = key(event);
        String id = EventIdConverter.idOf(event);
        synchronized (this) {
            Entry entry = entries.get(key);
            if (entry != null) {
                entry.suppressed++;
                entry.lastId = id;
                return;
            }
            entries.put(key, new Entry(event, id));
            appenders.appendLoopOnAppenders(event);
        }
    }

    @Override
    public void stop() {
        if (entries != null) {
            synchronized (this) {
                entries.values().forEach(this::writeSummary);
                entries.clear();
            }
        }
        appenders.detachAndStopAllAppenders();
        super.stop();
    }

    private void writeSummary(Entry entry) {
        if (entry.suppressed == 0) {
            return;
        }
        String message = "repeated " + entry.suppressed + " more times (first #" + entry.firstId + ", last #" + entry.lastId + "): " + entry.message;
        Logger logger = ((ch.qos.logback.classic.LoggerContext) getContext()).getLogger(entry.loggerName);
        appenders.appendLoopOnAppenders(new LoggingEvent(DeduplicatingAppender.class.getName(), logger, entry.level, message, null, null));
        entry.suppressed = 0;
    }

    private static String key(ILoggingEvent event) {
        StringBuilder key = new StringBuilder().append(event.getLevel()).append('|').append(event.getLoggerName()).append('|');
        IThrowableProxy root = rootCause(event.getThrowableProxy());
        if (root != null) {
            key.append(root.getClassName()).append(':').append(root.getMessage());
        } else {
            key.append(event.getFormattedMessage());
        }
        return key.toString();
    }

    private static String describe(ILoggingEvent event) {
        IThrowableProxy root = rootCause(event.getThrowableProxy());
        String message = event.getFormattedMessage();
        return root == null ? message : message + " | " + root.getClassName() + ": " + root.getMessage();
    }

    private static IThrowableProxy rootCause(IThrowableProxy throwable) {
        while (throwable != null && throwable.getCause() != null) {
            throwable = throwable.getCause();
        }
        return throwable;
    }

    private static String abbreviate(String message) {
        if (message == null) {
            return "";
        }
        String singleLine = message.replace('\n', ' ').replace('\r', ' ');
        return singleLine.length() > MAX_SUMMARY_MESSAGE ? singleLine.substring(0, MAX_SUMMARY_MESSAGE) + "..." : singleLine;
    }

    @Override
    public void addAppender(Appender<ILoggingEvent> appender) {
        appenders.addAppender(appender);
    }

    @Override
    public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
        return appenders.iteratorForAppenders();
    }

    @Override
    public Appender<ILoggingEvent> getAppender(String name) {
        return appenders.getAppender(name);
    }

    @Override
    public boolean isAttached(Appender<ILoggingEvent> appender) {
        return appenders.isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        appenders.detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(Appender<ILoggingEvent> appender) {
        return appenders.detachAppender(appender);
    }

    @Override
    public boolean detachAppender(String name) {
        return appenders.detachAppender(name);
    }
}
