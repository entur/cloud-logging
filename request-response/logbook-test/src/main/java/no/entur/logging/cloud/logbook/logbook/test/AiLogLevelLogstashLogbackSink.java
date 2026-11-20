package no.entur.logging.cloud.logbook.logbook.test;

import no.entur.logging.cloud.logbook.LogLevelLogstashLogbackSink;
import no.entur.logging.cloud.logbook.MessageComposer;
import no.entur.logging.cloud.logbook.RemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.RequestResponseSingleFieldAppendingMarker;
import org.slf4j.Marker;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * AI mode: the machine-readable sink, so message and JSON fields are the same as without AI mode, with the whole textual body
 * attached for the AI files (see {@link AiBodies}).
 */
public class AiLogLevelLogstashLogbackSink extends LogLevelLogstashLogbackSink {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends LogLevelLogstashLogbackSink.Builder {

        @Override
        public AiLogLevelLogstashLogbackSink build() {
            return (AiLogLevelLogstashLogbackSink) super.build();
        }

        @Override
        protected AiLogLevelLogstashLogbackSink newSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled,
                                                        JsonMapper jsonMapper, int maxSize,
                                                        RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier,
                                                        MessageComposer server, MessageComposer client) {
            return new AiLogLevelLogstashLogbackSink(logConsumer, logLevelEnabled, jsonMapper, maxSize, remoteHttpMessageContextSupplier,
                    server, client);
        }
    }

    public AiLogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled, JsonMapper jsonMapper,
                                         int maxSize, RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier,
                                         MessageComposer server, MessageComposer client) {
        super(logConsumer, logLevelEnabled, jsonMapper, maxSize, remoteHttpMessageContextSupplier, server, client);
    }

    @Override
    public RequestResponseSingleFieldAppendingMarker createRequestMarker(HttpRequest request) {
        return AiBodies.attach(super.createRequestMarker(request), request);
    }

    @Override
    public RequestResponseSingleFieldAppendingMarker createResponseMarker(Correlation correlation, HttpResponse response) {
        return AiBodies.attach(super.createResponseMarker(correlation, response), response);
    }
}
