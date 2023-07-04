package no.entur.logging.cloud.logback.logstash.test.junit;

import ch.qos.logback.classic.Level;
import net.logstash.logback.marker.ObjectAppendingMarker;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;

@CaptureLogStatements
public class ExampleTest {

	private final static Logger classLogger = LoggerFactory.getLogger(ExampleTest.class);

	@Test
	public void testLogger(LogStatements statements) {
		assertThat(statements).hasSize(0);
		
		MDC.put("myKey", "myValue");
		try {
			classLogger.info("Vital clue");
		} finally {
			MDC.remove("myKey");
		}
		
		assertThat(statements).hasSize(1);
		assertThat(statements.getLast().getMdc()).containsKey("myKey");
		assertThat(statements.getLast().getMessage()).contains("clue");
		assertThat(statements.getMessages()).contains("Vital clue");

		LogStatement firstLogStatement = statements.get(0);
		firstLogStatement.assertThatField("level").isEqualTo("INFO");
		assertThat(firstLogStatement.getLogLevel()).isEqualTo(Level.INFO);
	}

	@Test
	public void testLoggerWithMarker(LogStatements statements) {
		assertThat(statements).hasSize(0);
		
		Map<String, Object> map = new HashMap<>();
		map.put("name", "fedora");
		map.put("type", "linux");
		map.put("memory", "32GB");
		
		MDC.put("myKey", "myValue");
		try {
			classLogger.info(new ObjectAppendingMarker("system", map), "Vital clue");
		} finally {
			MDC.remove("myKey");
		}
		
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getMdc()).containsKey("myKey");
		assertThat(statements.get(0).getMessage()).contains("clue");
		assertThat(statements.getMessages()).contains("Vital clue");
		
		statements.get(0).assertThatField("system").field("memory").isEqualTo("32GB");
	}

	@Test
	public void testLoggerLogLevel(LogStatements statements) {
		classLogger.info("info");
		classLogger.warn("warn");
		classLogger.error("error");

		assertThat(statements).hasSize(3);
		assertThat(filterLogLevel(statements, Level.INFO)).hasSize(1);
		assertThat(filterLogLevel(statements, Level.WARN)).hasSize(1);
		assertThat(filterLogLevel(statements, Level.ERROR)).hasSize(1);
	}

	private List<LogStatement> filterLogLevel(LogStatements allLogs, Level desiredLogLevel) {
		return allLogs.stream()
			.filter(log -> log.getLogLevel().equals(desiredLogLevel))
			.collect(Collectors.toList()
			);
	}

}
