package no.entur.logging.cloud.logbook.ondemand;

import com.fasterxml.jackson.core.*;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageStateResult;
import no.entur.logging.cloud.logbook.util.MaxSizeJsonFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MaxSizeLocalHttpMessageBodyWriter implements HttpMessageBodyWriter {

    protected final byte[] input;
    protected final int maxSize;
    protected final JsonFactory jsonFactory;
    protected final MaxSizeJsonFilter maxSizeFilter;

    protected HttpMessageStateResult output;

    public MaxSizeLocalHttpMessageBodyWriter(JsonFactory jsonFactory, byte[] input, int maxSize) {
        this.jsonFactory = jsonFactory;
        this.input = input;
        this.maxSize = maxSize;

        this.maxSizeFilter = new MaxSizeJsonFilter(maxSize, jsonFactory);
    }

    public void prepareResult() {
        output = createOutput();
    }

    protected HttpMessageStateResult createOutput() {
        String wellformed = filterMaxSize(input);

        if (wellformed != null) {
            return new HttpMessageStateResult(true, wellformed);
        } else {
            String result = new String(input, 0, Math.min(maxSize, input.length), StandardCharsets.UTF_8);
            return new HttpMessageStateResult(false, result);
        }
    }

    @Override
    public void writeBody(JsonGenerator generator) throws IOException {
        if(output == null) {
            prepareResult();
        }
        HttpMessageStateResult output = this.output;

        if(output.isWellformed()) {
            generator.writeFieldName("body");
            generator.writeRawValue(output.getBody());
        } else {
            generator.writeStringField("body", output.getBody());
        }
    }

    protected String filterMaxSize(byte[] body) {
        try {
            return maxSizeFilter.transform(body);
        } catch (Exception e) {
            // NO-OP
            return null;
        }
    }

}
