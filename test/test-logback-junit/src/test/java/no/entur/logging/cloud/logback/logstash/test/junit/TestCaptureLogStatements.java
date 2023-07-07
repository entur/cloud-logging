package no.entur.logging.cloud.logback.logstash.test.junit;

import no.entur.logging.cloud.api.DevOpsLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
public class TestCaptureLogStatements {

	@CaptureLogStatements({"no.entur", "org.entur", "no"})
	public static class OverlappingClass  {
		
	}

	@CaptureLogStatements({"org.entur.logging.junit.any"})
	public static class SomeClass  {
		
	}

	@Test
	public void testOverlappingPackages() {
		LogbackTestExtension extension = new LogbackTestExtension();
		
		assertThrows(Exception.class, ()->{
				extension.postProcessTestInstance(new OverlappingClass(), null);
		});
		
	}

	@Test
	public void testNonCapturedClassLogger() throws Exception {
		LogbackTestExtension extension = new LogbackTestExtension();
		
		extension.postProcessTestInstance(new SomeClass(), null);
		
		ClassLogger classLogger = mock(ClassLogger.class);
		when(classLogger.level()).thenReturn(DevOpsLevel.INFO);
		when(classLogger.value()).thenReturn(new Class[] {TestCaptureLogStatements.class});

		assertThrows(Exception.class, ()->{
			extension.createLogStatementProxy(classLogger);
		});
	}

	@Test
	public void testNonCapturedPackageLogger() throws Exception {
		LogbackTestExtension extension = new LogbackTestExtension();
		
		extension.postProcessTestInstance(new SomeClass(), null);
		
		PackageLogger classLogger = mock(PackageLogger.class);
		when(classLogger.level()).thenReturn(DevOpsLevel.INFO);
		when(classLogger.value()).thenReturn(new String[] {"org.entur.logging.junit"});

		assertThrows(Exception.class, ()->{
			extension.createLogStatementProxy(classLogger);
		});

	}	
}
