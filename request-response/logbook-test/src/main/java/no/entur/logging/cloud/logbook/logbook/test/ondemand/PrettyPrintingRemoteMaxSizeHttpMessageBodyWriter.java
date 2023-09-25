package no.entur.logging.cloud.logbook.logbook.test.ondemand;

import com.fasterxml.jackson.core.JsonFactory;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageState;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageStateResult;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageStateSupplier;

import java.nio.charset.StandardCharsets;

public class PrettyPrintingRemoteMaxSizeHttpMessageBodyWriter extends PrettyPrintingLocalMaxSizeHttpMessageBodyWriter {

    protected final HttpMessageStateSupplier httpMessageStateSupplier;

    public PrettyPrintingRemoteMaxSizeHttpMessageBodyWriter(JsonFactory jsonFactory, byte[] input, int maxSize, HttpMessageStateSupplier httpMessageStateSupplier) {
        super(jsonFactory, input, maxSize);
        this.httpMessageStateSupplier = httpMessageStateSupplier;
    }

    protected HttpMessageStateResult createResult() {
        HttpMessageState httpMessageState = httpMessageStateSupplier.getHttpMessageState();

        if(httpMessageState == HttpMessageState.UNKNOWN || httpMessageState == HttpMessageState.VALID) {
            return super.createResult();
        } else {
            // do not attempt to parse
            String result = new String(input, 0, Math.min(maxSize, input.length), StandardCharsets.UTF_8);
            return new HttpMessageStateResult(false, result);
        }
    }


}
