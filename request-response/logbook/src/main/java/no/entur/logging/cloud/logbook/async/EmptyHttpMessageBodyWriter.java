package no.entur.logging.cloud.logbook.async;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

public class EmptyHttpMessageBodyWriter implements HttpMessageBodyWriter {

    public static final EmptyHttpMessageBodyWriter INSTANCE = new EmptyHttpMessageBodyWriter();

    @Override
    public void prepareWriteBody() {
        // do nothing
    }

    @Override
    public void writeBody(JsonGenerator generator) throws IOException {
        // do nothing
    }
}
