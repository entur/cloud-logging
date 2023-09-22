package no.entur.logging.cloud.logbook.async.state;

public class DefaultHttpMessageStateSupplier implements HttpMessageStateSupplier {

    private volatile HttpMessageState httpMessageState = HttpMessageState.UNKNOWN;

    public void setBodySyntaxState(HttpMessageState httpMessageState) {
        this.httpMessageState = httpMessageState;
    }

    @Override
    public HttpMessageState getHttpMessageState() {
        return httpMessageState;
    }
}
