package no.entur.logging.cloud.azure.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.joran.spi.DefaultClass;
import net.logstash.logback.composite.AbstractNestedJsonProvider;
import net.logstash.logback.composite.JsonProvider;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.composite.loggingevent.MdcJsonProvider;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;

public class AzureLogstashEncoder extends LoggingEventCompositeJsonEncoder {

    @Override
    @DefaultClass(LoggingEventJsonProviders.class)
    public void setProviders(JsonProviders<ILoggingEvent> jsonProviders) {

        if(AzureOpenTelemetryTraceMdcJsonProvider.isOtelAgent()) {
            // replace MDC provider with our own which translates agent trace_id and span_id to traceId and spanId
            // see https://docs.azure.cn/en-us/spring-apps/basic-standard/structured-app-log
            replaceMdcProvider(jsonProviders);
        }

        AzureServiceContextJsonProvider azureServiceContextJsonProvider = new AzureServiceContextJsonProvider();
        azureServiceContextJsonProvider.autodetectService();
        jsonProviders.addProvider(azureServiceContextJsonProvider);
        super.setProviders(jsonProviders);
    }

    @SuppressWarnings("unchecked")
    private boolean replaceMdcProvider(JsonProviders<ILoggingEvent> providers) {
        for (JsonProvider<ILoggingEvent> jsonProvider : providers.getProviders()) {
            if (jsonProvider instanceof MdcJsonProvider) {
                providers.removeProvider(jsonProvider);
                providers.addProvider(new AzureOpenTelemetryTraceMdcJsonProvider());
                return true;
            }
            if (jsonProvider instanceof AbstractNestedJsonProvider) {
                JsonProviders<ILoggingEvent> nested = ((AbstractNestedJsonProvider<ILoggingEvent>) jsonProvider).getProviders();
                if (replaceMdcProvider(nested)) {
                    return true;
                }
            }
        }
        return false;
    }



}
