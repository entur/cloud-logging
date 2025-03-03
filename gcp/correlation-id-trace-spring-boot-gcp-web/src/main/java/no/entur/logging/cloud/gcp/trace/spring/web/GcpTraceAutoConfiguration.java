package no.entur.logging.cloud.gcp.trace.spring.web;

import jakarta.servlet.DispatcherType;
import no.entur.logging.cloud.trace.spring.web.CorrelationIdAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GcpTraceAutoConfiguration {

    @Bean
    @ConditionalOnMissingClass("io.opentelemetry.api.OpenTelemetry")
    public FilterRegistrationBean<GcpTraceFilter> gcpTraceServletFilter() {
        FilterRegistrationBean<GcpTraceFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new GcpTraceFilter());
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);

        /*
         * Get the order value of this object.
         * <p>Higher values are interpreted as lower priority. As a consequence,
         * the object with the lowest value has the highest priority (somewhat
         * analogous to Servlet {@code load-on-startup} values).
         */
        registration.setOrder(CorrelationIdAutoConfiguration.ORDER + 1); // so before security (spring security at -100, see property spring.security.filter.order)
        registration.addUrlPatterns("/*");
        return registration;

    }

}
