package no.entur.logging.cloud.logbook.ondemand;

import tools.jackson.core.JsonGenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class StringHttpMessageBodyWriter implements HttpMessageBodyWriter {

    protected final String input;

    public StringHttpMessageBodyWriter(String input) {
        this.input = input;
    }

    @Override
    public void prepareResult() {
        // do nothing
    }

    @Override
    public void writeBody(JsonGenerator generator) throws IOException {
        generator.writeStringProperty("body", input);
    }

}
