package no.entur.logging.cloud.logbook.ondemand.state;

public class HttpMessageStateResult {

    public HttpMessageStateResult(boolean wellformed, String body) {
        this.body = body;
        this.wellformed = wellformed;
    }

    private boolean wellformed;
    private String body;

    public boolean isWellformed() {
        return wellformed;
    }

    public String getBody() {
        return body;
    }
}
