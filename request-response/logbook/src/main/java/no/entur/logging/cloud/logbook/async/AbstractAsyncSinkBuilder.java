package no.entur.logging.cloud.logbook.async;

import no.entur.logging.cloud.logbook.AbstractSinkBuilder;
import no.entur.logging.cloud.logbook.async.state.RequestHttpMessageStateSupplierSource;
import no.entur.logging.cloud.logbook.async.state.ResponseHttpMessageStateSupplierSource;

public abstract class AbstractAsyncSinkBuilder<B, E extends AbstractAsyncSinkBuilder<B, E>> extends AbstractSinkBuilder<B, E> {

    protected RequestHttpMessageStateSupplierSource requestBodyWellformedDecisionSupplier;
    protected ResponseHttpMessageStateSupplierSource responseBodyWellformedDecisionSupplier;


    @SuppressWarnings("unchecked")
    public B withValidateRequestJsonBodyWellformed(RequestHttpMessageStateSupplierSource validateRequestJsonBodyWellformed) {
        this.requestBodyWellformedDecisionSupplier = validateRequestJsonBodyWellformed;
        return (B) this;
    }

    @SuppressWarnings("unchecked")
    public B withValidateResponseJsonBodyWellformed(ResponseHttpMessageStateSupplierSource validateResponseJsonBodyWellformed) {
        this.responseBodyWellformedDecisionSupplier = validateResponseJsonBodyWellformed;
        return (B) this;
    }


}
