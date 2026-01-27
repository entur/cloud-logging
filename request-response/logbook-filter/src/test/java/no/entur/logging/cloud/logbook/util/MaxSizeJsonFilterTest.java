package no.entur.logging.cloud.logbook.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.CharArrayWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MaxSizeJsonFilterTest {

    private static final int MAX_BODY_SIZE = 14 * 1024;
    private final MaxSizeJsonFilter filter = new MaxSizeJsonFilter(MAX_BODY_SIZE, new JsonFactory());

    @Test
    public void testFilterTooBig() throws IOException {
        String s = generateLongJson(2 * MAX_BODY_SIZE);

        assertTrue(s.length() > MAX_BODY_SIZE);

        String transform = filter.transform(s);

        assertTrue(transform.length() <= MAX_BODY_SIZE);

    }

    @Test
    public void testFilterNotTooBig() throws IOException {
        String s = generateLongJson(MAX_BODY_SIZE / 2);

        assertFalse(s.length() > MAX_BODY_SIZE);

        String transform = filter.transform(s);

        assertEquals(s, transform);
    }

    @Test
    public void testFilterTooBigInvalidJson() throws IOException {
        JsonValidator jsonValidator = new JsonValidator(new JsonFactory());

        String s = generateLongJson(2 * MAX_BODY_SIZE);

        String invalidJson = s.substring(0, s.length() - 1);
        Assertions.assertFalse(jsonValidator.isWellformedJson(invalidJson));

        assertTrue(invalidJson.length() > MAX_BODY_SIZE);

        String transform = filter.transform(invalidJson);

        assertTrue(transform.length() <= MAX_BODY_SIZE);

        Assertions.assertTrue(jsonValidator.isWellformedJson(transform));
    }

    private String generateLongJson(int size) throws IOException {
        JsonFactory factory = new JsonFactory();

        CharArrayWriter writer = new CharArrayWriter();

        int chunkSize = 50;

        JsonGenerator generator = factory.createGenerator(writer);

        generator.writeStartObject();
        generator.writeStringField("start", "here");
        for(int i = 0; i < size; i += chunkSize) {
            generator.writeStringField("longValue", generateLongString(chunkSize));
        }
        generator.writeStringField("end", "here");
        generator.writeEndObject();

        generator.flush();

        return writer.toString();
    }


    private String generateLongString(int length) {
        StringBuilder builder = new StringBuilder(length);

        int mod = 'z' - 'a';

        for(int i = 0; i < length; i++) {
            char c = (char) ('a' + i % mod);
            builder.append(c);
        }
        return builder.toString();
    }

}
