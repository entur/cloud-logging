package no.entur.logging.cloud.logbook.async;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DefaultHttpMessageBodyWriter implements HttpMessageBodyWriter {

    protected final byte[] input;

    public DefaultHttpMessageBodyWriter(byte[] input) {
        this.input = input;
    }

    @Override
    public void prepareWriteBody() {
        // do nothing
    }

    @Override
    public void writeBody(JsonGenerator generator) throws IOException {
        generator.writeFieldName("body");
        generator.writeRawValue(new String(input, StandardCharsets.UTF_8));
    }


}
