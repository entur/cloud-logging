package no.entur.logging.cloud.logbook.ondemand;

import no.entur.logging.cloud.logbook.AbstractLogLevelSink;
import no.entur.logging.cloud.logbook.MessageComposer;
import no.entur.logging.cloud.logbook.RemoteHttpMessageContextSupplier;
import no.entur.logging.cloud.logbook.ondemand.state.RequestHttpMessageStateSupplierSource;
import no.entur.logging.cloud.logbook.ondemand.state.ResponseHttpMessageStateSupplierSource;
import org.slf4j.Marker;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public abstract class AbstractOndemandLogLevelLogstashLogbackSink extends AbstractLogLevelSink {

    protected final RequestHttpMessageStateSupplierSource requestHttpMessageStateSupplierSource;
    protected final ResponseHttpMessageStateSupplierSource responseHttpMessageStateSupplierSource;

    protected final RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier;

    protected JsonMapper jsonMapper;
    protected final int maxBodySize;
    protected final int maxSize;

    public AbstractOndemandLogLevelLogstashLogbackSink(BiConsumer<Marker, String> logConsumer,
            BooleanSupplier logLevelEnabled, JsonMapper jsonMapper,
            RequestHttpMessageStateSupplierSource requestHttpMessageStateSupplierSource,
            ResponseHttpMessageStateSupplierSource responseHttpMessageStateSupplierSource, int maxBodySize, int maxSize,
            RemoteHttpMessageContextSupplier remoteHttpMessageContextSupplier, MessageComposer server,
            MessageComposer client) {
        super(logLevelEnabled, logConsumer, server, client);
        this.jsonMapper = jsonMapper;
        this.requestHttpMessageStateSupplierSource = requestHttpMessageStateSupplierSource;
        this.responseHttpMessageStateSupplierSource = responseHttpMessageStateSupplierSource;
        this.maxBodySize = maxBodySize;
        this.maxSize = maxSize;
        this.remoteHttpMessageContextSupplier = remoteHttpMessageContextSupplier;
    }

}
