package no.entur.logging.cloud.logback.logstash.test.junit;

import no.entur.logging.cloud.logback.logstash.test.junit.dummy.Parent1;
import no.entur.logging.cloud.logback.logstash.test.junit.dummy.Parent2;
import no.entur.logging.cloud.logback.logstash.test.junit.dummy.sub.Child1;
import no.entur.logging.cloud.logback.logstash.test.junit.dummy.sub.Child2;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

@CaptureLogStatements({"no.entur.logging.cloud.logback.logstash.test.junit.dummy.sub"})
public class LogbackTestExtensionChildTest {

	@Test
	public void testLogger(LogStatements statements) {
		new Parent1().run();
		new Parent2().run();
		new Child1().run();
		new Child2().run();
		
		assertThat(statements).hasSize(6);
		assertThat(statements.get(0).getMessage()).isEqualTo("Child 1");
	}
	
	@Test
	public void testSpecificClass(@ClassLogger(Child2.class) LogStatements statements) {
		new Parent1().run();
		new Parent2().run();
		new Child1().run();
		new Child2().run();
		
		assertThat(statements).hasSize(3);
		assertThat(statements.get(0).getMessage()).isEqualTo("Child 2");
	}
}
