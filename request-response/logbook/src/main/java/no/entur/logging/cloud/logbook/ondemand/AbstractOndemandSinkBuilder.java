package no.entur.logging.cloud.logbook.ondemand;

import no.entur.logging.cloud.logbook.AbstractSinkBuilder;
import no.entur.logging.cloud.logbook.ondemand.state.RequestHttpMessageStateSupplierSource;
import no.entur.logging.cloud.logbook.ondemand.state.ResponseHttpMessageStateSupplierSource;

public abstract class AbstractOndemandSinkBuilder<B, E extends AbstractOndemandSinkBuilder<B, E>> extends AbstractSinkBuilder<B, E> {

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
