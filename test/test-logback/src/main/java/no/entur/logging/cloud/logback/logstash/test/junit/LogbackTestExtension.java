package no.entur.logging.cloud.logback.logstash.test.junit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import net.logstash.logback.encoder.LogstashEncoder;
import no.entur.logging.cloud.api.DevOpsLevel;
import no.entur.logging.cloud.api.DevOpsMarker;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleAppender;
import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
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

public class LogbackTestExtension extends LogbackInitializerExtension implements ParameterResolver, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

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

	protected static class Entry {
		private Logger logger;
		private ListAppender appender;

		public Entry(Logger logger, ListAppender appender) {
			this.logger = logger;
			this.appender = appender;
		}
	}

	protected List<Entry> entries = Collections.synchronizedList(new ArrayList<>());
	protected boolean postProcessed = false;
	protected long flushDelay = 500;
	protected DevOpsLevel defaultLevel;

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
		// at this point, some classes might already have been logging due to
		// other extensions, like spring boot extension and so on.
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		for(Entry entry : entries) {
			entry.logger.detachAppender(entry.appender);
			
			entry.appender.stop();
		}
		entries.clear();
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
			if(value.startsWith(entry.logger.getName())) {
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
		return add(name, severity);
	}

	protected ListAppender add(String name, Level level) {
		ListAppender appender = new ListAppender();
		appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
		appender.setName(name);
		appender.start();

		Logger logger = (Logger) LoggerFactory.getLogger(name);

		logger.addAppender(appender);
		logger.setLevel(level);

		// if set to false, there is no log output
		logger.setAdditive(true); // https://examples.javacodegeeks.com/enterprise-java/logback/logback-additivity-example/
		
		entries.add(new Entry(logger, appender));
		
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
		Logger logger = (Logger) LoggerFactory.getLogger(name);
		
		for(Entry entry : entries) {
			if(entry.logger == logger) {
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
	
	protected LogstashEncoder getEncoder() {
		Iterator<Appender<ILoggingEvent>> appenderIterator = LOGGER.iteratorForAppenders();
		while (appenderIterator.hasNext()) {
			Appender<ILoggingEvent> appender = appenderIterator.next();
			if(appender instanceof CompositeConsoleAppender) {
				CompositeConsoleAppender compositeConsoleAppender = (CompositeConsoleAppender)appender;
				return (LogstashEncoder) compositeConsoleAppender.getMachineReadableJsonEncoder();
			}
		}

		throw new IllegalStateException("Unable to get encoder");
	}
	
	public long getFlushDelay() {
		return flushDelay;
	}
	
	public void setFlushDelay(long flushDelay) {
		this.flushDelay = flushDelay;
	}
}
