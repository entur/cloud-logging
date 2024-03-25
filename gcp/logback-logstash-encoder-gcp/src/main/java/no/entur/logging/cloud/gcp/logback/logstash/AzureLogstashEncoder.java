package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.joran.spi.DefaultClass;
import net.logstash.logback.LogstashFormatter;
import net.logstash.logback.composite.AbstractCompositeJsonFormatter;
import net.logstash.logback.composite.JsonProvider;
import net.logstash.logback.composite.JsonProviders;
import net.logstash.logback.composite.loggingevent.LogLevelJsonProvider;
import net.logstash.logback.composite.loggingevent.LogLevelValueJsonProvider;
import net.logstash.logback.composite.loggingevent.LoggingEventJsonProviders;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;
import net.logstash.logback.composite.loggingevent.StackTraceJsonProvider;
import net.logstash.logback.composite.loggingevent.TagsJsonProvider;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;
import net.logstash.logback.encoder.LogstashEncoder;

import java.util.ArrayList;
import java.util.List;

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
