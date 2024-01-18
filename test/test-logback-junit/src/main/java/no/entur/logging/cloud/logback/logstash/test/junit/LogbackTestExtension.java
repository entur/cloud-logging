package no.entur.logging.cloud.logback.logstash.test.junit;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.AppenderAttachable;
import net.logstash.logback.encoder.CompositeJsonEncoder;
import no.entur.logging.cloud.api.DevOpsLevel;
import no.entur.logging.cloud.api.DevOpsMarker;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleAppender;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleAsyncAppenderLogging;
import no.entur.logging.cloud.logback.logstash.test.ILoggingEventListener;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LogbackTestExtension extends LogbackInitializerExtension implements ParameterResolver, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ILoggingEventListener {

	// implementation note: If there is a CompositeConsoleAsyncAppenderLogging available, connect to it directly so
	// that we get the log statements which are actually flushed (written) to console (include on-demand aspect).
	// Also we do not want multiple appenders if using multiple MDC sources; this avoid wrapping
	// logging events to get the extra MDC fields just for test support.

	private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(LogbackTestExtension.class);

	public static final int toLevelInteger(DevOpsLevel devOpsLevel) {
		switch(devOpsLevel) {
			case TRACE : return Level.TRACE_INT;
			case DEBUG : return Level.DEBUG_INT;
			case INFO : return Level.INFO_INT;
			case WARN : return Level.WARN_INT;
			case ERROR : return Level.ERROR_INT;
			case ERROR_TELL_ME_TOMORROW : return 50000;
			case ERROR_INTERRUPT_MY_DINNER : return 60000;
			case ERROR_WAKE_ME_UP_RIGHT_NOW : return 70000;
		}
		return Level.TRACE_INT;
	}

	public static final Level toLevel(DevOpsLevel devOpsLevel) {
		switch(devOpsLevel) {
			case TRACE : return Level.TRACE;
			case DEBUG : return Level.DEBUG;
			case INFO : return Level.INFO;
			case WARN : return Level.WARN;
			case ERROR :
			case ERROR_TELL_ME_TOMORROW:
			case ERROR_INTERRUPT_MY_DINNER:
			case ERROR_WAKE_ME_UP_RIGHT_NOW :
				return Level.ERROR;
		}
		return Level.TRACE;
	}
	public static final int toLevelInteger(ILoggingEvent event) {

		switch(event.getLevel().toInt()) {
			case Level.OFF_INT: return Level.OFF_INT;

			// 1-to-1 mappings:
			case Level.TRACE_INT: return Level.TRACE_INT;
			case Level.DEBUG_INT: return Level.DEBUG_INT;
			case Level.INFO_INT: return Level.INFO_INT;
			case Level.WARN_INT: return Level.WARN_INT;

			// 1-to-n mappings:
			case Level.ERROR_INT: {

				Marker marker = event.getMarker();
				if(marker != null) {
					DevOpsLevel level = DevOpsMarker.searchSeverityMarker(marker);
					if(level != null) {
						return toLevelInteger(level);
					}
				}
				return Level.ERROR_INT;
			}
		}

		// should never happen
		return Level.INFO_INT;
	}

	@Override
	public void put(ILoggingEvent event) {
		for (Entry entry : entries) {
			entry.add(event);
		}
	}

	protected static class Entry {
		private String name;
		private ListAppender appender;

		private Level level;

		private Logger logger;

		public Entry(String name, ListAppender appender, Level level) {
			this.name = name;
			this.appender = appender;
			this.level = level;
		}

		public void add(ILoggingEvent event) {
			if(event.getLoggerName().startsWith(name) && event.getLevel().isGreaterOrEqual(level)) {
				appender.append(event);
			}
		}

		public String getName() {
			return name;
		}
	}

	protected List<Entry> entries = Collections.synchronizedList(new ArrayList<>());
	protected boolean postProcessed = false;
	protected long flushDelay = 500;
	protected DevOpsLevel defaultLevel;

	protected boolean connected = false;

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		super.beforeAll(context);
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		for(Entry entry : entries) {
			entry.appender.clearEvents();
		}
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		if(!connected) {
			connected = connect();
		}
		if(connected) {
			for (Entry entry : entries) {
				entry.appender.start();
			}
		} else {
			for (Entry entry : entries) {
				if(entry.logger == null) {
					entry.logger = (Logger) LoggerFactory.getLogger(entry.name);
					entry.logger.addAppender(entry.appender);
					// if set to false, there is no log output
					entry.logger.setAdditive(true); // https://examples.javacodegeeks.com/enterprise-java/logback/logback-additivity-example/
				}

				entry.appender.start();
			}
		}
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		for(Entry entry : entries) {
			entry.appender.stop();
			if(entry.logger != null) {
				entry.logger.detachAppender(entry.appender);
			}
		}
		entries.clear();

		if(connected) {
			disconnect();

			connected = true;
		}
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		Class<?> type = parameterContext.getParameter().getType();
        if(type == LogbackTestExtension.class || type == LogStatements.class || type == LogbackLogStatements.class) {
            return true;
        }

		return false;
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		
		Class<?> type = parameterContext.getParameter().getType();
        if(type == LogbackTestExtension.class) {
            return this;
        } else if(type == LogStatements.class || type == LogbackLogStatements.class) {
			Optional<? extends ClassLogger> classLoggerAnnotation = parameterContext.findAnnotation(ClassLogger.class);
			if (classLoggerAnnotation.isPresent()) {
				return createLogStatementProxy(classLoggerAnnotation.get());
			}

			Optional<? extends PackageLogger> packageLoggerAnnotation = parameterContext.findAnnotation(PackageLogger.class);
			if (packageLoggerAnnotation.isPresent()) {
				return createLogStatementProxy(packageLoggerAnnotation.get());
			}

			return new LogbackLogStatements(defaultLevel, this);
		}

        throw new RuntimeException();
	}

	protected Object createLogStatementProxy(ClassLogger annotation) {
		Class<?>[] value = annotation.value();
		String[] names = new String[value.length];
		for (int i = 0; i < value.length; i++) {
			names[i] = value[i].getName();
			if(!isLogged(names[i])) {
				throw new IllegalArgumentException("Class '" + names[i] + "' not a child of configured root capture packages");
			}
		}
		return new LogbackLogStatements(names, annotation.level(), this);
	}

	protected Object createLogStatementProxy(PackageLogger annotation) {
		for (String string : annotation.value()) {
			if(!isLogged(string)) {
				throw new IllegalArgumentException("Package '" + string + "' not a child of configured root capture packages");
			}
		}
		return new LogbackLogStatements(annotation.value(), annotation.level(), this);
	}

	private boolean isLogged(String value) {
		for (Entry entry : entries) {
			if(value.startsWith(entry.getName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
		super.postProcessTestInstance(testInstance, context);
		
		if(!postProcessed) {
			postProcessed = true;

			// check test-class annotation for target classes
			List<Class<?>> list = new ArrayList<Class<?>>();
			Class<?> c = testInstance.getClass();
			do {
				list.add(c);
				c = c.getSuperclass();
			} while(c != null);
	
			Map<String, DevOpsLevel> loggers = new HashMap<>();
			for (int i = list.size() - 1; i >= 0; i--) {
				Class<?> clazz = list.get(i);
				CaptureLogStatements targets = clazz.getAnnotation(CaptureLogStatements.class);
				if(targets != null) {
					for (String target : targets.value()) {
						loggers.put(target, targets.level());
					}
					this.defaultLevel = targets.level();
				}
			}
	
			// check that top-level keys are not overlapping
			// since we need to have additivity true (statements are passed to parent appenders)
			// for console output, this would duplicate some of the log statements
			List<String> keys = new ArrayList<String>(loggers.keySet());
			for (int j = 0; j < keys.size(); j++) {
				for (int i = 0; i < keys.size(); i++) {
					if(i != j) {
						if(keys.get(i).startsWith(keys.get(j)) || keys.get(j).startsWith(keys.get(i))) {
							throw new IllegalArgumentException("Overlapping top-level packages not supported: " + keys.get(i) + " and " + keys.get(j));
						}
					}
				}
			}
			
			for (Map.Entry<String, DevOpsLevel> entry : loggers.entrySet()) {
				add(entry.getKey(), entry.getValue());
			}
		}
	}
	
	public ListAppender add(Class<?> cls, Level level) {
		return add(cls.getName(), level);
	}

	protected ListAppender add(String name, DevOpsLevel severity) {
		return add(name, toLevel(severity));
	}

	protected ListAppender add(String name, Level level) {
		ListAppender appender = new ListAppender();
		appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
		appender.setName(name);
		appender.start();

		entries.add(new Entry(name, appender, level));
		
		return appender;
	}

	@Override
	public String toString() {
		int count = 0;
		for(Entry entry : entries) {
			count += entry.appender.size();
		}
		
		return getClass().getSimpleName() + "[" + count + " captured statements]";
	}
	
	protected List<ILoggingEvent> nextEvents(String name) {
		for(Entry entry : entries) {
			if(entry.name.equals(name)) {
				return entry.appender.nextEvents(null);
			}
		}
		
		throw new IllegalArgumentException("Unable to find logger for " + name);
	}

	public List<ILoggingEvent> nextEvents() {
		if(entries.size() == 1) {
			return entries.get(0).appender.nextEvents(null);
		}
		List<ILoggingEvent> result = new ArrayList<ILoggingEvent>();
		for(Entry entry : entries) {
			result.addAll(entry.appender.nextEvents(null));
		}
		Collections.sort(result, ListAppender.loggingEventTimestampComparator);
		return result;
	}

	public List<ILoggingEvent> nextEvents(DevOpsLevel severity) {
		if(entries.size() == 1) {
			return entries.get(0).appender.nextEvents(severity);
		}
		List<ILoggingEvent> result = new ArrayList<ILoggingEvent>();
		for(Entry entry : entries) {
			result.addAll(entry.appender.nextEvents(severity));
		}
		Collections.sort(result, ListAppender.loggingEventTimestampComparator);
		return result;
	}

	protected CompositeJsonEncoder searchMachineReadableJsonEncoder(Iterator<Appender<ILoggingEvent>> appenderIterator) {
		while (appenderIterator.hasNext()) {
			Appender<ILoggingEvent> appender = appenderIterator.next();
			if (appender instanceof CompositeConsoleAppender<ILoggingEvent>) {
				CompositeConsoleAppender<ILoggingEvent> compositeConsoleAppender = (CompositeConsoleAppender<ILoggingEvent>) appender;
				return (CompositeJsonEncoder) compositeConsoleAppender.getMachineReadableJsonEncoder();
			}
		}
		return null;
	}

	protected CompositeJsonEncoder searchCompositeJsonEncoder(Iterator<Appender<ILoggingEvent>> appenderIterator) {
		while (appenderIterator.hasNext()) {
			Appender<ILoggingEvent> appender = appenderIterator.next();
			if(appender instanceof ConsoleAppender<ILoggingEvent>) {
				ConsoleAppender<ILoggingEvent> compositeConsoleAppender = (ConsoleAppender<ILoggingEvent>)appender;
				Encoder<ILoggingEvent> encoder = compositeConsoleAppender.getEncoder();
				if(encoder instanceof CompositeJsonEncoder) {
					return (CompositeJsonEncoder) encoder;
				}
			}
		}
		return null;
	}

	protected boolean connect() {
		Logger logger = LOGGER.getLoggerContext().getLogger(Logger.ROOT_LOGGER_NAME);

		Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders();
		while (appenderIterator.hasNext()) {
			Appender<ILoggingEvent> appender = appenderIterator.next();
			if (appender instanceof CompositeConsoleAsyncAppenderLogging a) {
				a.setListener(this);

				return true;
			}
		}
		return false;
	}

	protected void disconnect() {
		Logger logger = LOGGER.getLoggerContext().getLogger(Logger.ROOT_LOGGER_NAME);

		Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders();
		while (appenderIterator.hasNext()) {
			Appender<ILoggingEvent> appender = appenderIterator.next();
			if (appender instanceof CompositeConsoleAsyncAppenderLogging a) {
				a.setListener(null);
			}
		}
	}


	protected CompositeJsonEncoder getEncoder() {
		Logger logger = LOGGER.getLoggerContext().getLogger(Logger.ROOT_LOGGER_NAME);

		CompositeJsonEncoder compositeJsonEncoder = searchMachineReadableJsonEncoder(logger.iteratorForAppenders());
		if(compositeJsonEncoder != null) {
			return compositeJsonEncoder;
		}
		compositeJsonEncoder = searchCompositeJsonEncoder(logger.iteratorForAppenders());
		if(compositeJsonEncoder != null) {
			return compositeJsonEncoder;
		}

		// drill into async appender
		Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders();
		while (appenderIterator.hasNext()) {
			Appender<ILoggingEvent> appender = appenderIterator.next();
			if(appender instanceof AppenderAttachable) {
				AppenderAttachable asyncAppender = (AppenderAttachable)appender;

				 compositeJsonEncoder = searchMachineReadableJsonEncoder(asyncAppender.iteratorForAppenders());
				if(compositeJsonEncoder != null) {
					return compositeJsonEncoder;
				}
				compositeJsonEncoder = searchCompositeJsonEncoder(asyncAppender.iteratorForAppenders());
				if(compositeJsonEncoder != null) {
					return compositeJsonEncoder;
				}
			}

		}
		throw new IllegalStateException("Unable to get Appender with LogstashEncoder encoder, has logback initialize yet?");
	}
	
	public long getFlushDelay() {
		return flushDelay;
	}
	
	public void setFlushDelay(long flushDelay) {
		this.flushDelay = flushDelay;
	}
}
