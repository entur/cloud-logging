package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
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
    protected AbstractCompositeJsonFormatter<ILoggingEvent> createFormatter() {
        AbstractCompositeJsonFormatter formatter = (AbstractCompositeJsonFormatter) super.createFormatter();

        JsonProviders loggingEventJsonProviders = formatter.getProviders();
        List<JsonProvider<ILoggingEvent>> providers = new ArrayList<>(loggingEventJsonProviders.getProviders());

        for (JsonProvider<ILoggingEvent> jsonProvider : providers) {
            System.out.println(jsonProvider);
        }

        return formatter;
    }
}
