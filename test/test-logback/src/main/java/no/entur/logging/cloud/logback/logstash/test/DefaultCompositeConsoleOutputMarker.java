package no.entur.logging.cloud.logback.logstash.test;

import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import org.slf4j.Marker;

import java.io.IOException;

// TODO make immutable
public class DefaultCompositeConsoleOutputMarker extends LogstashMarker implements CompositeConsoleOutputMarker {

    // cached instances for lower overhead
    private static final DefaultCompositeConsoleOutputMarker HUMAN_READABLE_PLAIN = new DefaultCompositeConsoleOutputMarker(CompositeConsoleOutputType.humanReadablePlain);
    private static final DefaultCompositeConsoleOutputMarker HUMAN_READABLE_JSON = new DefaultCompositeConsoleOutputMarker(CompositeConsoleOutputType.humanReadableJson);
    private static final DefaultCompositeConsoleOutputMarker MACHINE_READABLE_JSON = new DefaultCompositeConsoleOutputMarker(CompositeConsoleOutputType.machineReadableJson);

    public static Marker getCurrentCompositeOutput() {

        CompositeConsoleOutputType output = CompositeConsoleOutputControl.getOutput();
        switch (output) {
            case humanReadablePlain: {
                return HUMAN_READABLE_PLAIN;
            }
            case humanReadableJson: {
                return HUMAN_READABLE_JSON;
            }
            case machineReadableJson: {
                return MACHINE_READABLE_JSON;
            }
            default: {
                throw new IllegalStateException();
            }
        }
    }

    public static final String MARKER_NAME = SingleFieldAppendingMarker.MARKER_NAME_PREFIX + "OUTPUT";
    private final CompositeConsoleOutputType compositeConsoleOutputType;

    private DefaultCompositeConsoleOutputMarker(CompositeConsoleOutputType compositeConsoleOutputType) {
        super(MARKER_NAME);

        this.compositeConsoleOutputType = compositeConsoleOutputType;;
    }

    @Override
    public void writeTo(JsonGenerator generator) throws IOException {
        // noop
    }

    @Override
    public CompositeConsoleOutputType getCompositeConsoleOutputType() {
        return compositeConsoleOutputType;
    }
}
