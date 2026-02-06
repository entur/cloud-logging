package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import tools.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import no.entur.logging.cloud.api.DevOpsLevel;
import no.entur.logging.cloud.api.DevOpsMarker;
import org.slf4j.Marker;

import java.io.IOException;
import java.util.Iterator;

/**
 * Change the 'level' to 'severity' in the JSON log statement so that it is picked up by Stackdriver.
 * 
 * @see <a href="https://stackoverflow.com/questions/30955142/logseverity-on-aggregated-logs-in-google-container-engine">https://stackoverflow.com/questions/30955142/logseverity-on-aggregated-logs-in-google-container-engine</a>
 * @see <a href="https://github.com/kubernetes/kubernetes/issues/13355">https://github.com/kubernetes/kubernetes/issues/13355</a>
 */

public class StackdriverLogSeverityJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> implements FieldNamesAware<LogstashFieldNames> {

    public static final String FIELD_SEVERITY = "severity";

    public static StackdriverSeverity getSeverity(final ILoggingEvent event) {
        Level level = event.getLevel();
        if (level == Level.TRACE) {
            return StackdriverSeverity.DEBUG;
        } else if (level == Level.DEBUG) {
            return StackdriverSeverity.DEBUG;
        } else if (level == Level.INFO) {
            return StackdriverSeverity.INFO;
        } else if (level == Level.WARN) {
            return StackdriverSeverity.WARNING;
        } else if (level == Level.ERROR) {
            // set custom error level, i.e. emergency, alert, critical
            Marker marker = event.getMarker();
            if(marker != null) {
                StackdriverSeverity severity = searchSeverityMarker(marker);
                if(severity != null) {
                    return severity;
                }
            }

            return StackdriverSeverity.ERROR;
        } else if (level == Level.ALL) {
            return StackdriverSeverity.DEBUG;
        }

        return StackdriverSeverity.DEFAULT;
    }
        
    public static StackdriverSeverity searchSeverityMarker(Marker marker) {
        if (marker instanceof DevOpsMarker) {
            DevOpsMarker stackdriverErrorMarker = (DevOpsMarker) marker;

            DevOpsLevel devOpsLevel = stackdriverErrorMarker.getDevOpsLevel();

            return StackdriverSeverity.forDevOpsLevel(devOpsLevel);
        } else if(marker.hasReferences()) {
            Iterator<Marker> iterator = marker.iterator();
            while(iterator.hasNext()) {
                StackdriverSeverity severity = searchSeverityMarker(iterator.next());
                if(severity != null) {
                    return severity;
                }
            }
        }
        return null;
    }

    public StackdriverLogSeverityJsonProvider() {
        setFieldName(FIELD_SEVERITY);
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) {
        JsonWritingUtils.writeStringField(generator, getFieldName(), getSeverity(event).toString());
    }
    
    @Override
    public void setFieldNames(LogstashFieldNames fieldNames) {
    }
    
    
}