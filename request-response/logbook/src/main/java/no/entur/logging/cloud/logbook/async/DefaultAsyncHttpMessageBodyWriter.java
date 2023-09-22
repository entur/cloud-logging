package no.entur.logging.cloud.logbook.async;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import no.entur.logging.cloud.logbook.async.state.HttpMessageState;
import no.entur.logging.cloud.logbook.async.state.HttpMessageStateOutput;
import no.entur.logging.cloud.logbook.async.state.HttpMessageStateSupplier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DefaultAsyncHttpMessageBodyWriter extends DefaultHttpMessageBodyWriter {

    protected final HttpMessageStateSupplier httpMessageStateSupplier;

    protected volatile HttpMessageStateOutput output;
    protected final JsonFactory jsonFactory;

    public DefaultAsyncHttpMessageBodyWriter(JsonFactory jsonFactory, byte[] input, HttpMessageStateSupplier httpMessageStateSupplier) {
        super(input);

        this.jsonFactory = jsonFactory;
        this.httpMessageStateSupplier = httpMessageStateSupplier;
    }

    @Override
    public void prepareWriteBody() {
        output = createOutput();
    }
    public HttpMessageStateOutput createOutput() {
        HttpMessageState httpMessageState = httpMessageStateSupplier.getHttpMessageState();

        if(httpMessageState == HttpMessageState.UNKNOWN) {
            if(isWellformedJson()) {
                httpMessageState = HttpMessageState.VALID;
            } else {
                httpMessageState = HttpMessageState.INVALID;
            }
        }
        return new HttpMessageStateOutput(httpMessageState == HttpMessageState.VALID, new String(input, StandardCharsets.UTF_8));
    }

    @Override
    public void writeBody(JsonGenerator generator) throws IOException {
        HttpMessageStateOutput output = this.output;
        if(output == null) {
            this.output = output = createOutput();
        }
        if(output.isWellformed()) {
            generator.writeFieldName("body");
            generator.writeRawValue(output.getOutput());
        } else {
            generator.writeStringField("body", output.getOutput());
        }
    }

    protected boolean isWellformedJson() {
        try (JsonParser parser = jsonFactory.createParser(new ByteArrayInputStream(input))) {
            while(parser.nextToken() != null);
        } catch(Exception e) {
            return false;
        }
        return true;
    }

}
