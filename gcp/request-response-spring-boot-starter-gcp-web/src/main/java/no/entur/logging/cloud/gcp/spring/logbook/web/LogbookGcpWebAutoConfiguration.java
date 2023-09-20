package no.entur.logging.cloud.gcp.spring.logbook.web;

import jakarta.servlet.http.HttpServletRequest;
import no.entur.logging.cloud.gcp.spring.web.OndemandFilter;
import no.entur.logging.cloud.logbook.WellformedRequestBodyDecisionSupplier;
import no.entur.logging.cloud.spring.logbook.LogbookLoggingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.concurrent.atomic.AtomicBoolean;

@Configuration
@AutoConfigureBefore(LogbookLoggingAutoConfiguration.class)
@PropertySource(value = "classpath:logbook.gcp.properties", ignoreResourceNotFound = false)
public class LogbookGcpWebAutoConfiguration {

    /**
     *
     * Make it so that request which contain well-formed JSON does not trigger a
     * validation of wellformed JSON when creation of the log statement.
     *
     * This is a considerable reduction in the work on the logger thread.
     *
     *  TODO make this work also when ondemand is disabled
     */

    @Configuration
    @ConditionalOnProperty(name = {"entur.logging.http.ondemand.enabled"}, havingValue = "true", matchIfMissing = false)
    public static class OndemandConfiguration {

        @Bean
        @ConditionalOnMissingBean(WellformedRequestBodyDecisionSupplier.class)
        public WellformedRequestBodyDecisionSupplier requestBodyWellformedDecisionSupplier(HttpServletRequest context) {

            return () -> {
                // note: we are now within the request context, but the returned BooleanSupplier will probably be
                // outside the request context when it is invoked next time
                Object attribute = context.getAttribute(OndemandFilter.WELLFORMED_INDICATOR);
                if(attribute instanceof AtomicBoolean result) {
                    return () -> result.get();
                }

                return () -> false;
            };
        }

        @Bean
        public WebMvcRegistrations customMvcRegistrations() {
            return new WellformedRequestBodyWebMvcRegistrations();
        }

    }
}