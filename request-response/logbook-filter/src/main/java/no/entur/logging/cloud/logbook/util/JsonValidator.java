package no.entur.logging.cloud.logbook.util;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.json.JsonMapper;

import java.io.StringReader;

public class JsonValidator {

    private final JsonMapper jsonMapper;

    public JsonValidator(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public boolean isWellformedJson(String input) {
        try (JsonParser parser = jsonMapper.createParser(new StringReader(input))) {
            while(parser.nextToken() != null);
        } catch(Exception e) {
            return false;
        }
        return true;
    }
}
