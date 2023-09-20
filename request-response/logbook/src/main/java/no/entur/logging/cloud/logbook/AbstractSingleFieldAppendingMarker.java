package no.entur.logging.cloud.logbook;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import org.zalando.logbook.ContentType;
import org.zalando.logbook.HttpMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

public abstract class AbstractSingleFieldAppendingMarker<T extends HttpMessage> extends SingleFieldAppendingMarker {

    protected final BooleanSupplier wellformed;

    protected final int maxBodySize;
    protected final int maxSize;

    protected String contentType;
    protected Map<String, List<String>> headers;
    protected byte[] body;

    public AbstractSingleFieldAppendingMarker(String markerName, BooleanSupplier wellformed, T message, int maxBodySize, int maxSize) {
        super(markerName, "http");
        this.wellformed = wellformed;
        this.maxBodySize = maxBodySize;
        this.maxSize = maxSize;

        prepareForDeferredProcessing(message);
    }

    protected void prepareForDeferredProcessing(T message) {
        // some value as read directly from underlying web object, which might be recycled before
        // this marker is written
        // capture all interesting fields

        contentType = message.getContentType();
        headers = message.getHeaders();
        try {
            body = message.getBody();
        } catch (IOException e) {
            body = null;
        }
    }

    protected void writeBody(JsonGenerator generator) {
        if(body != null && ContentType.isJsonMediaType(contentType)) {
            // check that body is well-formed and within size

            // TODO get the generator.getOutputTarget() and calculate the size allocated to
            // fixed fields and headers
            // using generator.getOutputBuffered() + output.length()
            // while there technically can be multiple markers involved here,
            // this is not the case for these logbook-related statements

            int max = Math.min(maxSize, maxBodySize);

            try {
                boolean confirmedWellformedJsonBody = this.wellformed.getAsBoolean();

                JsonFactory factory = generator.getCodec().getFactory();
                if(body.length < max) {
                    boolean escape = !confirmedWellformedJsonBody && !isWellformedJson(body, factory);

                    if (escape) {
                        // escape body as string for debugging
                        generator.writeStringField("body", new String(body, StandardCharsets.UTF_8));
                    } else {
                        generator.writeFieldName("body");
                        writeApprovedBody(generator, body);
                    }
                } else {
                    // must parse body regardless, so do filtering and well-formed validation in a single operation
                    byte[] wellformed = filterMaxSize(body, factory, max);

                    if (wellformed != null) {
                        generator.writeFieldName("body");
                        writeApprovedBody(generator, wellformed);
                    } else {
                        // escape body as string for debugging
                        generator.writeStringField("body", new String(body, 0, max));
                    }
                }
            } catch(Exception e) {
                // should never happen, this is probably going to blow up somewhere else
            }
        }
        // omit writing body
    }

    protected void writeApprovedBody(JsonGenerator generator, byte[] body) throws IOException {
        generator.writeRawValue(new String(body, StandardCharsets.UTF_8));
    }

    protected boolean isWellformedJson(byte[] s, JsonFactory jsonFactory) {
        try (JsonParser parser = jsonFactory.createParser(new ByteArrayInputStream(s))) {
            while(parser.nextToken() != null);
        } catch(Exception e) {
            return false;
        }
        return true;
    }

    protected void writeHeaders(JsonGenerator generator) throws IOException {
        generator.writeFieldName("headers");
        generator.writeStartObject();
        for (Map.Entry<String, List<String>> stringListEntry : headers.entrySet()) {
            generator.writeFieldName(stringListEntry.getKey().toLowerCase());
            generator.writeStartArray();
            for(String value : stringListEntry.getValue()) {
                generator.writeString(value);
            }
            generator.writeEndArray();
        }
        generator.writeEndObject();
    }

    @Override
    protected T getFieldValue() {
        return null;
    }


    protected byte[] filterMaxSize(byte[] body, JsonFactory factory, int max) {
        try (
                JsonParser parser = factory.createParser(body);
                ByteArrayOutputStream writer = new ByteArrayOutputStream(max);
                JsonGenerator generator = factory.createGenerator(writer);
        ) {
            process(parser, generator, () -> generator.getOutputBuffered() + writer.size(), max);

            generator.flush();
            return writer.toByteArray();
        } catch (Exception e) {
            // NO-OP
            return null;
        }
    }

    public void process(final JsonParser parser, JsonGenerator generator, LongSupplier outputSizeSupplier, int max) throws IOException {
        String message = "Max body size of " + max + " reached, rest of the document has been filtered.";

        final long limit = max - message.length() - 8 - 16; // filtering is not completely accurate

        // do not write field name if field value is too long
        String fieldName = null;

        long inputOffset = 0;

        while(true) {
            JsonToken nextToken = parser.nextToken();
            if(nextToken == null) {
                break;
            }
            if(nextToken == JsonToken.FIELD_NAME) {
                fieldName = parser.currentName();

                continue;
            }

            if(nextToken == JsonToken.VALUE_STRING) {
                parser.getTextLength();
            }

            long nextInputOffset = parser.currentLocation().getCharOffset();

            long size;
            if(nextToken == JsonToken.VALUE_STRING) {
                size = parser.getTextLength() + 2;
                if(fieldName != null) {
                    size += fieldName.length() + 2;
                }
            } else {
                size = nextInputOffset - inputOffset; // i.e. this includes whitespace
            }
            long outputSize = outputSizeSupplier.getAsLong();

            if(outputSize + size >= limit) {
                // write notification

                JsonStreamContext ctxt = generator.getOutputContext();
                if (ctxt.inArray()) {
                    generator.writeString("Logger: " + message);
                } else if (ctxt.inObject()) {
                    generator.writeStringField("Logger", message);
                }

                generator.close();

                break;
            }

            if(fieldName != null) {
                generator.writeFieldName(fieldName);
                fieldName = null;
            }

            generator.copyCurrentEvent(parser);

            inputOffset = nextInputOffset;
        }
        generator.flush();
    }

}
