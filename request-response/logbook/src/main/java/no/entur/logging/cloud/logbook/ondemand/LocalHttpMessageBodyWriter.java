package no.entur.logging.cloud.logbook.ondemand;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LocalHttpMessageBodyWriter implements HttpMessageBodyWriter {

    protected final byte[] input;

    public LocalHttpMessageBodyWriter(byte[] input) {
        this.input = input;
    }

    @Override
    public void prepareResult() {
        // do nothing
    }

    @Override
    public void writeBody(JsonGenerator generator) throws IOException {
        generator.writeFieldName("body");
        generator.writeRawValue(new String(input, StandardCharsets.UTF_8));
    }


}
