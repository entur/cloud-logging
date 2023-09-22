package no.entur.logging.cloud.logbook.async;

import com.fasterxml.jackson.core.JsonFactory;
import no.entur.logging.cloud.logbook.AbstractLogLevelSink;
import no.entur.logging.cloud.logbook.async.state.RequestHttpMessageStateSupplierSource;
import no.entur.logging.cloud.logbook.async.state.ResponseHttpMessageStateSupplierSource;
import org.slf4j.Marker;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public abstract class AbstractAsyncLogLevelLogstashLogbackSink extends AbstractLogLevelSink {

    protected final RequestHttpMessageStateSupplierSource requestHttpMessageStateSupplierSource;
    protected final ResponseHttpMessageStateSupplierSource responseHttpMessageStateSupplierSource;

    protected JsonFactory jsonFactory;
    protected final int maxBodySize;
    protected final int maxSize;

    public AbstractAsyncLogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer, BooleanSupplier logLevelEnabled, JsonFactory jsonFactory, RequestHttpMessageStateSupplierSource requestHttpMessageStateSupplierSource, ResponseHttpMessageStateSupplierSource responseHttpMessageStateSupplierSource, int maxBodySize, int maxSize) {
        super(logLevelEnabled, logConsumer);
        this.jsonFactory = jsonFactory;
        this.requestHttpMessageStateSupplierSource = requestHttpMessageStateSupplierSource;
        this.responseHttpMessageStateSupplierSource = responseHttpMessageStateSupplierSource;
        this.maxBodySize = maxBodySize;
        this.maxSize = maxSize;
    }



}
