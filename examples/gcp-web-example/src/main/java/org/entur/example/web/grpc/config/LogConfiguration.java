package org.entur.example.web.grpc.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.BodyFilter;
import org.zalando.logbook.json.JsonBodyFilters;

@Configuration
public class LogConfiguration {

	@Bean
	public BodyFilter filterBody() {
        final Set<String> properties = new HashSet<>();
        properties.add("secret");
        return JsonBodyFilters.replaceJsonStringProperty(properties, "hidden");
	}
	
}
