package no.entur.logging.cloud.gcp.trace.spring.web;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class GcpCorrelationIdAutoConfiguration {

    @Bean
    public FilterRegistrationBean<GcpCorrelationIdFilter> tracingServletFilter() {
        FilterRegistrationBean<GcpCorrelationIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new GcpCorrelationIdFilter());
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);

        /*
         * Get the order value of this object.
         * <p>Higher values are interpreted as lower priority. As a consequence,
         * the object with the lowest value has the highest priority (somewhat
         * analogous to Servlet {@code load-on-startup} values).
         */
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 100); // so before security (spring security at -100, see property spring.security.filter.order)
        registration.addUrlPatterns("/*");
        return registration;

    }

}
