package no.entur.logging.cloud.logbook.logbook.test.async;

import com.fasterxml.jackson.core.JsonFactory;
import no.entur.logging.cloud.logbook.async.*;
import no.entur.logging.cloud.logbook.async.state.HttpMessageStateSupplier;
import no.entur.logging.cloud.logbook.async.state.RequestHttpMessageStateSupplierSource;
import no.entur.logging.cloud.logbook.async.state.ResponseHttpMessageStateSupplierSource;
import org.slf4j.Marker;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 *
 * Sink which delays processes body (checking well-formed + filter on size) untill it is time to write the log statement(s).
 *
 */


public class PrettyPrintingAsyncLogLevelLogstashLogbackSink extends AbstractAsyncLogLevelLogstashLogbackSink {

    public static BuilderAsync newBuilder() {
        return new BuilderAsync();
    }

    public static class BuilderAsync extends AbstractAsyncSinkBuilder<BuilderAsync, BuilderAsync> {

        public PrettyPrintingAsyncLogLevelLogstashLogbackSink build() {
            if(maxBodySize == -1) {
                throw new IllegalStateException("Expected max body size");
            }
            if(maxSize == -1) {
                throw new IllegalStateException("Expected max size");
            }
            if(logger == null) {
                throw new IllegalStateException("Expected logger");
            }
            if(level == null) {
                throw new IllegalStateException("Expected log level");
            }
            if(jsonFactory == null) {
                jsonFactory = new JsonFactory();
            }
            return new PrettyPrintingAsyncLogLevelLogstashLogbackSink(loggerToBiConsumer(), logEnabledToBooleanSupplier(), jsonFactory, requestBodyWellformedDecisionSupplier, responseBodyWellformedDecisionSupplier, maxBodySize, maxSize);
        }
    }

    public PrettyPrintingAsyncLogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled, JsonFactory jsonFactory, RequestHttpMessageStateSupplierSource requestHttpMessageStateSupplierSource, ResponseHttpMessageStateSupplierSource responseHttpMessageStateSupplierSource, int maxBodySize, int maxSize) {
        super(logConsumer, logLevelEnabled, jsonFactory, requestHttpMessageStateSupplierSource, responseHttpMessageStateSupplierSource, maxBodySize, maxSize);
    }

    public Marker createRequestMarker(HttpRequest request) {
        HttpMessageBodyWriter writer = EmptyHttpMessageBodyWriter.INSTANCE;

        if(ContentType.isJsonMediaType(request.getContentType())) {
            try {
                byte[] body = request.getBody();
                if(body != null && body.length > 0) {
                    if (request.getOrigin().equals("local")) {
                        // trust our own data to be wellformed
                        if (body.length > maxBodySize) {
                            writer = new PrettyPrintingHttpMessageBodyWriter(body);
                        } else {
                            writer = new PrettyPrintingMaxSizeHttpMessageBodyWriter(jsonFactory, body, maxBodySize);
                        }
                    } else {
                        HttpMessageStateSupplier httpMessageStateSupplier = requestHttpMessageStateSupplierSource.get();
                        if (body.length > maxBodySize) {
                            writer = new PrettyPrintingAsyncHttpMessageBodyWriter(jsonFactory, body, httpMessageStateSupplier);
                        } else {
                            writer = new PrettyPrintingAsyncMaxSizeHttpMessageBodyWriter(jsonFactory, body, maxSize, httpMessageStateSupplier);
                        }
                    }
                }
            } catch (IOException e) {
                // ignore
            }
        }

        return new RequestAsyncSingleFieldAppendingMarker(request, writer);
    }

    public Marker createResponseMarker(Correlation correlation, HttpResponse response) {

        HttpMessageBodyWriter writer = EmptyHttpMessageBodyWriter.INSTANCE;

        if(ContentType.isJsonMediaType(response.getContentType())) {
            try {
                byte[] body = response.getBody();
                if(body != null && body.length > 0) {
                    if (response.getOrigin().equals("local")) {
                        // trust our own data to be wellformed
                        if (body.length > maxBodySize) {
                            writer = new PrettyPrintingHttpMessageBodyWriter(body);
                        } else {
                            writer = new PrettyPrintingMaxSizeHttpMessageBodyWriter(jsonFactory, body, maxBodySize);
                        }
                    } else {
                        HttpMessageStateSupplier httpMessageStateSupplier = responseHttpMessageStateSupplierSource.get();
                        if (body.length > maxBodySize) {
                            writer = new PrettyPrintingAsyncHttpMessageBodyWriter(jsonFactory, body, httpMessageStateSupplier);
                        } else {
                            writer = new PrettyPrintingAsyncMaxSizeHttpMessageBodyWriter(jsonFactory, body, maxSize, httpMessageStateSupplier);
                        }
                    }
                }
            } catch (IOException e) {
                // ignore
            }
        }

        return new ResponseAsyncSingleFieldAppendingMarker(response, correlation.getDuration().toMillis(), writer);
    }

}
