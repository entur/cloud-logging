package org.entur.example.web.config;

import java.util.HashSet;
import java.util.Set;

import no.entur.logging.cloud.spring.ondemand.web.scope.LoggingScopeThreadUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
