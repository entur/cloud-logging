package no.entur.logging.cloud.logback.logstash.test.junit;

import no.entur.logging.cloud.api.DevOpsLevel;
import no.entur.logging.cloud.api.DevOpsMarker;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.truth.Truth.assertThat;

@CaptureLogStatements({"no.entur", "org.entur"})
public class LogbackTestExtensionTest {

    private final static Logger classLogger = LoggerFactory.getLogger(LogbackTestExtensionTest.class);

    private final static Logger packageLogger = LoggerFactory.getLogger("no.entur.logging");
	
	@Test
	public void testClassLogger(@ClassLogger(LogbackTestExtensionTest.class) LogStatements statements) {
		packageLogger.info("First"); // should not register
		
		assertThat(statements).hasSize(0);
		
		classLogger.info("Second"); // should register
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getMessage()).isEqualTo("Second");
	}
	
	@Test
	public void testPackageLogger(@PackageLogger("no.entur.logging") LogStatements statements) {
		assertThat(statements).hasSize(0); 
		
		packageLogger.info("First"); // should register
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getMessage()).isEqualTo("First");
		
		classLogger.info("Second"); // should register

		assertThat(statements).hasSize(2);
		assertThat(statements.get(1).getMessage()).isEqualTo("Second");
	}
	
	@Test
	public void testClassLoggerForWarning(@ClassLogger(value = {LogbackTestExtensionTest.class}, level = DevOpsLevel.WARN) LogStatements statements) {
		classLogger.info("First"); // should register in top-level appenders, but be filtered out in this specific method.
		
		assertThat(statements).hasSize(0);
		
		classLogger.warn("Warning"); // should register
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getMessage()).isEqualTo("Warning");
	}
	
	@Test
	public void testClassLoggerDebugIsIgnoredForDefault(LogStatements statements) {
		classLogger.debug("First"); // should not register in top-level appenders.
		
		assertThat(statements).hasSize(0);
		
		classLogger.info("Info"); // should register
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getMessage()).isEqualTo("Info");
	}
	
	@Test
	public void testClassLoggerForError(@ClassLogger(value = {LogbackTestExtensionTest.class}, level = DevOpsLevel.ERROR) LogStatements statements) {
		classLogger.info("First"); // should register in top-level appenders, but be filtered out in this specific method.
		
		assertThat(statements).hasSize(0);
		
		classLogger.error("Error"); // should register
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getMessage()).isEqualTo("Error");
	}
	
	@Test
	public void testClassLoggerForCritical(@ClassLogger(value = {LogbackTestExtensionTest.class}, level = DevOpsLevel.ERROR_INTERRUPT_MY_DINNER) LogStatements statements) {
		classLogger.error("First"); // should register in top-level appenders, but be filtered out in this specific method.
		
		assertThat(statements).hasSize(0);
		
		classLogger.error(DevOpsMarker.errorInterruptMyDinner(), "Critical Error"); // should register
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getMessage()).isEqualTo("Critical Error");
	}

	@Test
	public void testClassLoggerForAlert(@ClassLogger(value = {LogbackTestExtensionTest.class}, level = DevOpsLevel.ERROR_WAKE_ME_UP_RIGHT_NOW) LogStatements statements) {
		classLogger.error(DevOpsMarker.errorInterruptMyDinner(), "First"); // should register in top-level appenders, but be filtered out in this specific method.
		
		assertThat(statements).hasSize(0);
		
		classLogger.error(DevOpsMarker.errorWakeMeUpRightNow(), "Alert Error"); // should register
		assertThat(statements).hasSize(1);
		assertThat(statements.get(0).getMessage()).isEqualTo("Alert Error");
	}

}
