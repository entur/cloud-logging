package no.entur.logging.cloud.logbook.async;

import com.fasterxml.jackson.core.JsonFactory;
import no.entur.logging.cloud.logbook.async.state.HttpMessageState;
import no.entur.logging.cloud.logbook.async.state.HttpMessageStateOutput;
import no.entur.logging.cloud.logbook.async.state.HttpMessageStateSupplier;

import java.nio.charset.StandardCharsets;

public class AsyncMaxSizeHttpMessageBodyWriter extends MaxSizeHttpMessageBodyWriter {

    protected final HttpMessageStateSupplier httpMessageStateSupplier;

    public AsyncMaxSizeHttpMessageBodyWriter(JsonFactory jsonFactory, byte[] input, int maxSize, HttpMessageStateSupplier httpMessageStateSupplier) {
        super(jsonFactory, input, maxSize);
        this.httpMessageStateSupplier = httpMessageStateSupplier;
    }

    protected HttpMessageStateOutput createOutput() {
        HttpMessageState httpMessageState = httpMessageStateSupplier.getHttpMessageState();

        if(httpMessageState == HttpMessageState.UNKNOWN || httpMessageState == HttpMessageState.VALID) {
            return super.createOutput();
        } else {
            // do not attempt to parse
            String result = new String(input, 0, maxSize, StandardCharsets.UTF_8);
            return new HttpMessageStateOutput(false, result);
        }
    }


}
