package no.entur.logging.cloud.logbook.ondemand;

import no.entur.logging.cloud.logbook.AbstractLogLevelLogstashLogbackSink;
import no.entur.logging.cloud.logbook.DefaultRemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.MessageComposer;
import no.entur.logging.cloud.logbook.RemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageStateSupplier;
import no.entur.logging.cloud.logbook.ondemand.state.RequestHttpMessageStateSupplierSource;
import no.entur.logging.cloud.logbook.ondemand.state.ResponseHttpMessageStateSupplierSource;
import org.slf4j.Marker;
import org.zalando.logbook.*;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 *
 * Sink which delays processes body (checking well-formed + filter on size)
 * until it is time to write the log statement(s).
 *
 */

public class OndemandLogLevelLogstashLogbackSink extends AbstractOndemandLogLevelLogstashLogbackSink {

    public static BuilderAsync newBuilder() {
        return new BuilderAsync();
    }

    public static class BuilderAsync extends AbstractOndemandSinkBuilder<BuilderAsync, BuilderAsync> {

        public OndemandLogLevelLogstashLogbackSink build() {
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
            if (jsonMapper == null) {
                jsonMapper = JsonMapper.builder().build();
            }
            if (remoteHttpMessageContextSupplier == null) {
                remoteHttpMessageContextSupplier = new DefaultRemoteHttpMessageContextSupplier();
            }
            return new OndemandLogLevelLogstashLogbackSink(loggerToBiConsumer(), logEnabledToBooleanSupplier(),
                    jsonMapper, requestBodyWellformedDecisionSupplier, responseBodyWellformedDecisionSupplier,
                    maxBodySize, maxSize, remoteHttpMessageContextSupplier, server, client);
        }
    }

    public OndemandLogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled,
                                               JsonMapper jsonMapper, RequestHttpMessageStateSupplierSource requestHttpMessageStateSupplierSource,
            ResponseHttpMessageStateSupplierSource responseHttpMessageStateSupplierSource, int maxBodySize, int maxSize,
            RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier, MessageComposer server,
            MessageComposer client) {
        super(logConsumer, logLevelEnabled, jsonMapper, requestHttpMessageStateSupplierSource,
                responseHttpMessageStateSupplierSource, maxBodySize, maxSize, remoteHttpMessageContextSupplier, server,
                client);
    }

    public Marker createRequestMarker(HttpRequest request) {
        HttpMessageBodyWriter writer = EmptyHttpMessageBodyWriter.INSTANCE;

        if (ContentType.isJsonMediaType(request.getContentType())) {
            try {
                byte[] body = request.getBody();
                if (body != null && body.length > 0) {
                    if (request.getOrigin() == Origin.LOCAL) {
                        // trust our own data to be wellformed and not pretty-printed
                        if (body.length < maxBodySize) {
                            writer = new LocalHttpMessageBodyWriter(body);
                        } else {
                            writer = new MaxSizeLocalHttpMessageBodyWriter(jsonMapper, body, maxBodySize);
                        }
                    } else {
                        boolean verify = remoteHttpMessageContextSupplier.verifyJsonSyntax(request);
                        if (verify) {
                            HttpMessageStateSupplier httpMessageStateSupplier = requestHttpMessageStateSupplierSource
                                    .get();
                            if (body.length < maxBodySize) {
                                writer = new RemoteHttpMessageBodyWriter(jsonMapper, body, httpMessageStateSupplier);
                            } else {
                                writer = new MaxSizeRemoteHttpMessageBodyWriter(jsonMapper, body, maxSize,
                                        httpMessageStateSupplier);
                            }
                        } else {
                            // trust contents is well formed
                            if (body.length < maxBodySize) {
                                writer = new LocalHttpMessageBodyWriter(body);
                            } else {
                                writer = new MaxSizeLocalHttpMessageBodyWriter(jsonMapper, body, maxBodySize);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        } else if (AbstractLogLevelLogstashLogbackSink.isXmlMediaType(request.getContentType())) {
            try {
                String bodyAsString = request.getBodyAsString();
                if (bodyAsString != null && bodyAsString.length() > 0) {
                    if (bodyAsString.length() > maxBodySize) {
                        String truncated = bodyAsString.substring(0, maxBodySize);
                        writer = new StringHttpMessageBodyWriter(truncated);
                    } else {
                        writer = new StringHttpMessageBodyWriter(bodyAsString);
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }

        return new RequestOndemandSingleFieldAppendingMarker(request, writer);
    }

    public Marker createResponseMarker(Correlation correlation, HttpResponse response) {

        HttpMessageBodyWriter writer = EmptyHttpMessageBodyWriter.INSTANCE;

        if (ContentType.isJsonMediaType(response.getContentType())) {
            try {
                byte[] body = response.getBody();
                if (body != null && body.length > 0) {
                    if (response.getOrigin() == Origin.LOCAL) {
                        // trust our own data to be wellformed and not pretty-printed
                        if (body.length < maxBodySize) {
                            writer = new LocalHttpMessageBodyWriter(body);
                        } else {
                            writer = new MaxSizeLocalHttpMessageBodyWriter(jsonMapper, body, maxBodySize);
                        }
                    } else {
                        boolean verify = remoteHttpMessageContextSupplier.verifyJsonSyntax(response);
                        if (verify) {
                            HttpMessageStateSupplier httpMessageStateSupplier = responseHttpMessageStateSupplierSource
                                    .get();
                            if (body.length < maxBodySize) {
                                writer = new RemoteHttpMessageBodyWriter(jsonMapper, body, httpMessageStateSupplier);
                            } else {
                                writer = new MaxSizeRemoteHttpMessageBodyWriter(jsonMapper, body, maxSize,
                                        httpMessageStateSupplier);
                            }
                        } else {
                            // trust contents is well formed
                            if (body.length < maxBodySize) {
                                writer = new LocalHttpMessageBodyWriter(body);
                            } else {
                                writer = new MaxSizeLocalHttpMessageBodyWriter(jsonMapper, body, maxBodySize);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        } else if (AbstractLogLevelLogstashLogbackSink.isXmlMediaType(response.getContentType())) {
            try {
                String bodyAsString = response.getBodyAsString();
                if (bodyAsString != null && bodyAsString.length() > 0) {
                    if (bodyAsString.length() > maxBodySize) {
                        String truncated = bodyAsString.substring(0, maxBodySize);
                        writer = new StringHttpMessageBodyWriter(truncated);
                    } else {
                        writer = new StringHttpMessageBodyWriter(bodyAsString);
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }

        return new ResponseOndemandSingleFieldAppendingMarker(response, correlation.getDuration(), writer);
    }

}
