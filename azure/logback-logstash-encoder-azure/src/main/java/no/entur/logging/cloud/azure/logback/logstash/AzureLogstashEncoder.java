package no.entur.logging.cloud.azure.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.joran.spi.DefaultClass;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;

public class AzureLogstashEncoder extends LoggingEventCompositeJsonEncoder {

    @Override
    @DefaultClass(LoggingEventJsonProviders.class)
    public void setProviders(JsonProviders<ILoggingEvent> jsonProviders) {
        AzureServiceContextJsonProvider azureServiceContextJsonProvider = new AzureServiceContextJsonProvider();
        azureServiceContextJsonProvider.autodetectService();
        jsonProviders.addProvider(azureServiceContextJsonProvider);
        super.setProviders(jsonProviders);
    }



}
