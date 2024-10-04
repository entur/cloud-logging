package no.entur.logging.cloud.azure.spring.logbook.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import no.entur.logging.cloud.azure.spring.ondemand.web.properties.OndemandProperties;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageState;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageStateSupplier;
import no.entur.logging.cloud.logbook.ondemand.state.RequestHttpMessageStateSupplierSource;
import no.entur.logging.cloud.logbook.ondemand.state.ResponseHttpMessageStateSupplierSource;
import no.entur.logging.cloud.spring.logbook.LogbookLoggingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import static jakarta.servlet.DispatcherType.ERROR;
import static jakarta.servlet.DispatcherType.REQUEST;

@Configuration
@AutoConfigureBefore(LogbookLoggingAutoConfiguration.class)
@PropertySource(value = "classpath:logbook.azure.web.properties", ignoreResourceNotFound = false)
public class LogbookAzureWebAutoConfiguration {

    /**
     *
     * If on-demand-logging is enabled, take advantage so that we always output correct JSON.
     *
     */

    @Configuration
    @ConditionalOnProperty(name = {"entur.logging.http.ondemand.enabled"}, havingValue = "true", matchIfMissing = false)
    @ConditionalOnClass(OndemandProperties.class)
    public static class OndemandConfiguration {

        @Bean
        public FilterRegistrationBean<HttpMessageStateFilter> httpMessageStateFilter(OndemandProperties properties) {
            FilterRegistrationBean<HttpMessageStateFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(new HttpMessageStateFilter());
            registration.setDispatcherTypes(REQUEST, ERROR);
            registration.setOrder(properties.getFilterOrder() - 1);
            registration.addUrlPatterns(properties.getFilterUrlPatterns());
            return registration;
        }

        @Bean
        @ConditionalOnMissingBean(RequestHttpMessageStateSupplierSource.class)
        public RequestHttpMessageStateSupplierSource requestHttpMessageStateSupplierSource(HttpServletRequest context) {

            return () -> {
                // note: we are now within the request context, but the returned BooleanSupplier will probably be
                // outside the request context when it is invoked next time
                Object attribute = context.getAttribute(HttpMessageStateFilter.HTTP_MESSAGE_STATE);
                if(attribute instanceof HttpMessageStateSupplier result) {
                    return () -> result.getHttpMessageState();
                }

                return () -> HttpMessageState.UNKNOWN;
            };
        }

        @Bean
        @ConditionalOnMissingBean(ResponseHttpMessageStateSupplierSource.class)
        public ResponseHttpMessageStateSupplierSource responseHttpMessageStateSupplierSource(HttpServletResponse context) {
            return () -> () -> HttpMessageState.UNKNOWN;
        }

        @Bean
        public WebMvcRegistrations customMvcRegistrations() {
            return new HttpMessageStateFilterMvcRegistrations();
        }

    }
}