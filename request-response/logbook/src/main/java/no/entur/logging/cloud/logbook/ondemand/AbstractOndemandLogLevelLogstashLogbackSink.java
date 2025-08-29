package no.entur.logging.cloud.logbook.ondemand;

import com.fasterxml.jackson.core.JsonFactory;
import no.entur.logging.cloud.logbook.AbstractLogLevelSink;
import no.entur.logging.cloud.logbook.MessageComposer;
import no.entur.logging.cloud.logbook.RemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.ondemand.state.RequestHttpMessageStateSupplierSource;
import no.entur.logging.cloud.logbook.ondemand.state.ResponseHttpMessageStateSupplierSource;
import org.slf4j.Marker;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public abstract class AbstractOndemandLogLevelLogstashLogbackSink extends AbstractLogLevelSink {

    protected final RequestHttpMessageStateSupplierSource requestHttpMessageStateSupplierSource;
    protected final ResponseHttpMessageStateSupplierSource responseHttpMessageStateSupplierSource;

    protected final RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier;

    protected JsonFactory jsonFactory;
    protected final int maxBodySize;
    protected final int maxSize;

    public AbstractOndemandLogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer,
            BooleanSupplier logLevelEnabled, JsonFactory jsonFactory,
            RequestHttpMessageStateSupplierSource requestHttpMessageStateSupplierSource,
            ResponseHttpMessageStateSupplierSource responseHttpMessageStateSupplierSource, int maxBodySize, int maxSize,
            RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier, MessageComposer server,
            MessageComposer client) {
        super(logLevelEnabled, logConsumer, server, client);
        this.jsonFactory = jsonFactory;
        this.requestHttpMessageStateSupplierSource = requestHttpMessageStateSupplierSource;
        this.responseHttpMessageStateSupplierSource = responseHttpMessageStateSupplierSource;
        this.maxBodySize = maxBodySize;
        this.maxSize = maxSize;
        this.remoteHttpMessageContextSupplier = remoteHttpMessageContextSupplier;
    }

}
