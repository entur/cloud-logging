package no.entur.logging.cloud.logback.logstash.test.junit;

import no.entur.logging.cloud.api.DevOpsLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface PackageLogger {
	
	String[] value();

	DevOpsLevel level() default DevOpsLevel.INFO;
	
}
