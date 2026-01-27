package no.entur.logging.cloud.logbook.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.StringReader;

public class JsonValidator {

    private final JsonFactory jsonFactory;

    public JsonValidator(JsonFactory jsonFactory) {
        this.jsonFactory = jsonFactory;
    }

    public boolean isWellformedJson(String input) {
        try (JsonParser parser = jsonFactory.createParser(new StringReader(input))) {
            while(parser.nextToken() != null);
        } catch(Exception e) {
            return false;
        }
        return true;
    }
}
