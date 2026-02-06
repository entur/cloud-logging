package no.entur.logging.cloud.logbook;

import org.slf4j.Marker;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 *
 * Sink which processes body (checks wellformed and filters on size) as the
 * marker is created.
 *
 */

public class LogLevelLogstashLogbackSink extends AbstractLogLevelLogstashLogbackSink {

    public LogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled,
                                       JsonMapper jsonMapper, int maxSize, RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier,
                                       MessageComposer server, MessageComposer client) {
        super(logConsumer, logLevelEnabled, jsonMapper, maxSize, remoteHttpMessageContextSupplier, server, client);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AbstractSinkBuilder<Builder, Builder> {

        public LogLevelLogstashLogbackSink build() {
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
            if (client == null) {
                throw new IllegalStateException("Expected client message composer");
            }
            if (server == null) {
                throw new IllegalStateException("Expected server message composer");
            }
            if (jsonMapper == null) {
                jsonMapper = JsonMapper.builder().build();
            }
            if (remoteHttpMessageContextSupplier == null) {
                remoteHttpMessageContextSupplier = new DefaultRemoteHttpMessageContextSupplier();
            }
            return new LogLevelLogstashLogbackSink(loggerToBiConsumer(), logEnabledToBooleanSupplier(), jsonMapper,
                    Math.min(maxBodySize, maxSize), remoteHttpMessageContextSupplier, server, client);
        }
    }

    @Override
    protected Marker newRequestSingleFieldAppendingMarker(HttpRequest request, String body, boolean wellformed) {
        return new RequestSingleFieldAppendingMarker(request, body, wellformed);
    }

    @Override
    protected Marker newResponseSingleFieldAppendingMarker(HttpResponse response, Duration duration, String body,
                                                           boolean wellformed) {
        return new ResponseSingleFieldAppendingMarker(response, duration, body, wellformed);
    }

}
