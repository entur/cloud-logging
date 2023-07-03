package no.entur.logging.cloud.logback.logstash.test.junit;

import no.entur.logging.cloud.api.DevOpsLevel;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(LogbackTestExtension.class)
public @interface CaptureLogStatements {
	
	/**
	 * Packages to capture. Values must not overlap.
	 * 
	 * @return array of package names
	 */
	String[] value() default {"no.entur", "org.entur"};
	
	DevOpsLevel level() default DevOpsLevel.INFO;
}
