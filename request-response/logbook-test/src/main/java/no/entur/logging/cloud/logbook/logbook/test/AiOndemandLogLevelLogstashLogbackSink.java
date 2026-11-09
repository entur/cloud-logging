package no.entur.logging.cloud.logbook.logbook.test;

import no.entur.logging.cloud.logbook.MessageComposer;
import no.entur.logging.cloud.logbook.RemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.RequestResponseSingleFieldAppendingMarker;
import no.entur.logging.cloud.logbook.ondemand.OndemandLogLevelLogstashLogbackSink;
import no.entur.logging.cloud.logbook.ondemand.state.RequestHttpMessageStateSupplierSource;
import no.entur.logging.cloud.logbook.ondemand.state.ResponseHttpMessageStateSupplierSource;
import org.slf4j.Marker;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * AI mode: the on-demand machine-readable sink, so message and JSON fields are the same as without AI mode, with the whole textual
 * body attached for the AI files (see {@link AiBodies}).
 */
public class AiOndemandLogLevelLogstashLogbackSink extends OndemandLogLevelLogstashLogbackSink {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends OndemandLogLevelLogstashLogbackSink.BuilderAsync {

        @Override
        public AiOndemandLogLevelLogstashLogbackSink build() {
            super.build(); // validates and sets defaults
            return new AiOndemandLogLevelLogstashLogbackSink(loggerToBiConsumer(), logEnabledToBooleanSupplier(), jsonMapper,
                    requestBodyWellformedDecisionSupplier, responseBodyWellformedDecisionSupplier, maxBodySize, maxSize,
                    remoteHttpMessageContextSupplier, server, client);
        }
    }

    public AiOndemandLogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled,
                                                 JsonMapper jsonMapper, RequestHttpMessageStateSupplierSource requestHttpMessageStateSupplierSource,
                                                 ResponseHttpMessageStateSupplierSource responseHttpMessageStateSupplierSource, int maxBodySize,
                                                 int maxSize, RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier,
                                                 MessageComposer server, MessageComposer client) {
        super(logConsumer, logLevelEnabled, jsonMapper, requestHttpMessageStateSupplierSource, responseHttpMessageStateSupplierSource,
                maxBodySize, maxSize, remoteHttpMessageContextSupplier, server, client);
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
