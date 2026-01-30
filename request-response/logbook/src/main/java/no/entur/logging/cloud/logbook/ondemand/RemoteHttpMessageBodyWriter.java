package no.entur.logging.cloud.logbook.ondemand;

import tools.jackson.core.JsonGenerator;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageState;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageStateResult;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageStateSupplier;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RemoteHttpMessageBodyWriter extends LocalHttpMessageBodyWriter {

    protected final HttpMessageStateSupplier httpMessageStateSupplier;

    protected volatile HttpMessageStateResult output;
    protected final JsonMapper jsonMapper;

    public RemoteHttpMessageBodyWriter(JsonMapper jsonMapper, byte[] input, HttpMessageStateSupplier httpMessageStateSupplier) {
        super(input);

        this.jsonMapper = jsonMapper;
        this.httpMessageStateSupplier = httpMessageStateSupplier;
    }

    @Override
    public void prepareResult() {
        output = createOutput();
    }
    public HttpMessageStateResult createOutput() {
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
            this.output = output = createOutput();
        }
        if(output.isWellformed()) {
            generator.writeName("body");
            generator.writeRawValue(output.getBody());
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
