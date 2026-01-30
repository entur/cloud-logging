package no.entur.logging.cloud.logbook.logbook.test.ondemand;

import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageState;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageStateResult;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageStateSupplier;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PrettyPrintingRemoteHttpMessageBodyWriter extends PrettyPrintingLocalHttpMessageBodyWriter {

    protected final HttpMessageStateSupplier httpMessageStateSupplier;

    protected volatile HttpMessageStateResult output;
    protected final JsonMapper jsonMapper;

    public PrettyPrintingRemoteHttpMessageBodyWriter(JsonMapper jsonMapper, byte[] input, HttpMessageStateSupplier httpMessageStateSupplier) {
        super(input);

        this.jsonMapper = jsonMapper;
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
            generator.writeName("body");

            PrettyPrinter prettyPrinter = generator.getPrettyPrinter();
            if (prettyPrinter == null) {
                generator.writeRawValue(output.getBody());
            } else {
                final TokenStreamFactory factory = generator.objectWriteContext().tokenStreamFactory();

                // append to existing tree event by event
                try (final JsonParser parser = jsonMapper.createParser(output.getBody())) {
                    while (parser.nextToken() != null) {
                        generator.copyCurrentEvent(parser);
                    }
                }
            }
        } else {
            generator.writeStringProperty("body", output.getBody());
        }
    }

    protected boolean isWellformedJson() {
        try (JsonParser parser = jsonMapper.createParser(new ByteArrayInputStream(input))) {
            while(parser.nextToken() != null);
        } catch(Exception e) {
            return false;
        }
        return true;
    }

}
