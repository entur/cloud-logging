package org.entur.example.web;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleOutputControl;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class HumanReadableJsonApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

	@Override 
	public void onApplicationEvent(ContextRefreshedEvent event) {
		CompositeConsoleOutputControl.useHumanReadableJsonEncoder();
	}
}
