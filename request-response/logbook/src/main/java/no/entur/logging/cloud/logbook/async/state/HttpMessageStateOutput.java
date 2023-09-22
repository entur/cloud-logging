package no.entur.logging.cloud.logbook.async.state;

public class HttpMessageStateOutput {

    public HttpMessageStateOutput(boolean wellformed, String output) {
        this.output = output;
        this.wellformed = wellformed;
    }

    private boolean wellformed;
    private String output;

    public boolean isWellformed() {
        return wellformed;
    }

    public String getOutput() {
        return output;
    }
}
