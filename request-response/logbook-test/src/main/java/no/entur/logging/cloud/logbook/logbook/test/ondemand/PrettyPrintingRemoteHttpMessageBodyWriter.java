package no.entur.logging.cloud.logbook.logbook.test.ondemand;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.PrettyPrinter;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageState;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageStateResult;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageStateSupplier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PrettyPrintingRemoteHttpMessageBodyWriter extends PrettyPrintingLocalHttpMessageBodyWriter {

    protected final HttpMessageStateSupplier httpMessageStateSupplier;

    protected volatile HttpMessageStateResult output;
    protected final JsonFactory jsonFactory;

    public PrettyPrintingRemoteHttpMessageBodyWriter(JsonFactory jsonFactory, byte[] input, HttpMessageStateSupplier httpMessageStateSupplier) {
        super(input);

        this.jsonFactory = jsonFactory;
        this.httpMessageStateSupplier = httpMessageStateSupplier;
    }

    @Override
    public void prepareResult() {
        output = createResult();
    }
    public HttpMessageStateResult createResult() {
        HttpMessageState httpMessageState = httpMessageStateSupplier.getHttpMessageState();

        if(httpMessageState == HttpMessageState.UNKNOWN) {
            if(isWellformedJson()) {
                httpMessageState = HttpMessageState.VALID;
            } else {
                httpMessageState = HttpMessageState.INVALID;
            }
        }
        return new HttpMessageStateResult(httpMessageState == HttpMessageState.VALID, new String(input, StandardCharsets.UTF_8));
    }

    @Override
    public void writeBody(JsonGenerator generator) throws IOException {
        HttpMessageStateResult output = this.output;
        if(output == null) {
            this.output = output = createResult();
        }
        if(output.isWellformed()) {
            generator.writeFieldName("body");

            PrettyPrinter prettyPrinter = generator.getPrettyPrinter();
            if (prettyPrinter == null) {
                generator.writeRawValue(output.getBody());
            } else {
                final JsonFactory factory = generator.getCodec().getFactory();

                // append to existing tree event by event
                try (final JsonParser parser = factory.createParser(output.getBody())) {
                    while (parser.nextToken() != null) {
                        generator.copyCurrentEvent(parser);
                    }
                }
            }
        } else {
            generator.writeStringField("body", output.getBody());
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
