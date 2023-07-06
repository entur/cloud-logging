package org.entur.logbook.example.config;

import org.entur.logbook.example.DemoApplication;
import org.entur.logbook.example.rest.DocumentEndpoint;
import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.stereotype.Component;

@Component
public class JerseyConfig extends ResourceConfig {

	public JerseyConfig() {
		register(DocumentEndpoint.class);
		packages(DemoApplication.class.getPackage().getName());
	}

}