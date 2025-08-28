package no.entur.logging.cloud.logbook.logbook.test;

import com.fasterxml.jackson.core.JsonFactory;
import no.entur.logging.cloud.logbook.AbstractLogLevelLogstashLogbackSink;
import no.entur.logging.cloud.logbook.AbstractSinkBuilder;
import no.entur.logging.cloud.logbook.DefaultRemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.MessageComposer;
import no.entur.logging.cloud.logbook.RemoteHttpMessageContextSupplier;
import org.slf4j.Marker;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public class PrettyPrintingLogLevelLogstashLogbackSink extends AbstractLogLevelLogstashLogbackSink {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AbstractSinkBuilder<Builder, Builder> {

        public PrettyPrintingLogLevelLogstashLogbackSink build() {
            if (maxBodySize == -1) {
                throw new IllegalStateException("Expected max body size");
            }
            if (maxSize == -1) {
                throw new IllegalStateException("Expected max size");
            }
            if (logger == null) {
                throw new IllegalStateException("Expected logger");
            }
            if (level == null) {
                throw new IllegalStateException("Expected log level");
            }
            if (jsonFactory == null) {
                jsonFactory = new JsonFactory();
            }
            if (remoteHttpMessageContextSupplier == null) {
                remoteHttpMessageContextSupplier = new DefaultRemoteHttpMessageContextSupplier();
            }
            return new PrettyPrintingLogLevelLogstashLogbackSink(loggerToBiConsumer(), logEnabledToBooleanSupplier(),
                    jsonFactory, Math.min(maxBodySize, maxSize), remoteHttpMessageContextSupplier, server, client);
        }
    }

    public PrettyPrintingLogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer,
            BooleanSupplier logLevelEnabled, JsonFactory jsonFactory, int maxSize,
            RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier, MessageComposer server,
            MessageComposer client) {
        super(logConsumer, logLevelEnabled, jsonFactory, maxSize, remoteHttpMessageContextSupplier, server, client);
    }

    @Override
    protected Marker newRequestSingleFieldAppendingMarker(HttpRequest request, String body, boolean wellformed) {
        return new PrettyPrintingRequestSingleFieldAppendingMarker(request, body, wellformed);
    }

    @Override
    protected Marker newResponseSingleFieldAppendingMarker(HttpResponse response, long millis, String body,
            boolean wellformed) {
        return new PrettyPrintingResponseSingleFieldAppendingMarker(response, millis, body, wellformed);
    }

}
