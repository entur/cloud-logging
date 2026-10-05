package no.entur.logging.cloud.protobuf.json;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Utility for checking that a {@linkplain ProtobufJsonWriter} writes the same JSON as {@linkplain JsonFormat}
 * for a given message, i.e. in a unit test before opting in to use another implementation than JsonFormat:
 *
 * <pre>
 * JsonFormatComparison comparison = JsonFormatComparison.newBuilder()
 *         .withTypeRegistry(typeRegistry)
 *         .withWriter(TranscodingProtobufJsonWriter.newBuilder().withTypeRegistry(typeRegistry).build())
 *         .build();
 *
 * comparison.verify(myMessage);
 * </pre>
 *
 * The outputs are compared as JSON values, so differences in escaping (JsonFormat escapes some characters, like
 * {@code =}, as unicode) or number notation do not count. Object properties are compared regardless of order;
 * duplicate properties are reported as differences.
 * <br><br>
 * Both byte-based (UTF-8) and char-based JSON generators are checked, as implementations might write
 * strings differently for the two.
 */

public final class JsonFormatComparison {

    private static final int MAX_DIFFERENCES = 20;

    private static final Object NULL = new Object() {
        @Override
        public String toString() {
            return "null";
        }
    };

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private JsonFormat.TypeRegistry typeRegistry = JsonFormat.TypeRegistry.getEmptyTypeRegistry();
        private ProtobufJsonWriter writer;

        /**
         * @param typeRegistry types for resolving google.protobuf.Any, for JsonFormat
         * @return this builder
         */

        public Builder withTypeRegistry(JsonFormat.TypeRegistry typeRegistry) {
            this.typeRegistry = typeRegistry;
            return this;
        }

        /**
         * @param writer the implementation to compare with JsonFormat
         * @return this builder
         */

        public Builder withWriter(ProtobufJsonWriter writer) {
            this.writer = writer;
            return this;
        }

        public JsonFormatComparison build() {
            if (writer == null) {
                throw new IllegalStateException("Expected writer");
            }
            JsonFormat.Printer printer = JsonFormat.printer().usingTypeRegistry(typeRegistry).omittingInsignificantWhitespace();
            return new JsonFormatComparison(printer, writer);
        }
    }

    /**
     * Result of a comparison.
     */

    public static final class Result {

        private final String expected;
        private final Exception expectedFailure;
        private final String actual;
        private final Exception actualFailure;
        private final List<String> differences;

        private Result(String expected, Exception expectedFailure, String actual, Exception actualFailure, List<String> differences) {
            this.expected = expected;
            this.expectedFailure = expectedFailure;
            this.actual = actual;
            this.actualFailure = actualFailure;
            this.differences = differences;
        }

        /**
         * @return true if the outputs are the same JSON, or both failed
         */

        public boolean isSame() {
            return differences.isEmpty();
        }

        /**
         * @return JsonFormat output, or null if it failed
         */

        public String getExpected() {
            return expected;
        }

        /**
         * @return JsonFormat failure, or null
         */

        public Exception getExpectedFailure() {
            return expectedFailure;
        }

        /**
         * @return writer output (byte-based generator), or null if it failed
         */

        public String getActual() {
            return actual;
        }

        /**
         * @return writer failure, or null
         */

        public Exception getActualFailure() {
            return actualFailure;
        }

        /**
         * @return differences, as JSON pointer path and description; empty if the same
         */

        public List<String> getDifferences() {
            return differences;
        }

        @Override
        public String toString() {
            if (isSame()) {
                return "Same as JsonFormat";
            }
            StringBuilder builder = new StringBuilder();
            builder.append("Different from JsonFormat: ").append(differences);
            builder.append("\nJsonFormat: ").append(expected != null ? expected : expectedFailure);
            builder.append("\nWriter:     ").append(actual != null ? actual : actualFailure);
            return builder.toString();
        }
    }

    private final JsonFormat.Printer printer;
    private final ProtobufJsonWriter writer;
    private final JsonFactory factory = JsonFactory.builder().build();

    private JsonFormatComparison(JsonFormat.Printer printer, ProtobufJsonWriter writer) {
        this.printer = printer;
        this.writer = writer;
    }

    /**
     * Compare the output of JsonFormat and the writer.
     *
     * @param message message or builder
     * @return result
     */

    public Result compare(MessageOrBuilder message) {
        String expected = null;
        Exception expectedFailure = null;
        try {
            expected = printer.print(message);
        } catch (Exception e) {
            expectedFailure = e;
        }

        String actual = null;
        String actualChars = null;
        Exception actualFailure = null;
        try {
            actual = writeBytes(message);
            actualChars = writeChars(message);
        } catch (Exception e) {
            actualFailure = e;
        }

        List<String> differences = new ArrayList<>();
        if (expectedFailure != null || actualFailure != null) {
            if (expectedFailure == null) {
                differences.add("Writer failed: " + actualFailure);
            } else if (actualFailure == null) {
                differences.add("JsonFormat failed: " + expectedFailure);
            }
            // both failing is the same behaviour
        } else {
            Object expectedValue = parse(expected, "", differences);
            Object actualValue = parse(actual, "", differences);
            diff("", expectedValue, actualValue, differences);

            Object actualCharsValue = parse(actualChars, "", differences);
            List<String> charDifferences = new ArrayList<>();
            diff("", expectedValue, actualCharsValue, charDifferences);
            for (String difference : charDifferences) {
                differences.add("(char-based generator) " + difference);
            }
        }
        return new Result(expected, expectedFailure, actual, actualFailure, differences.isEmpty() ? Collections.emptyList() : differences);
    }

    /**
     * Verify that the output of JsonFormat and the writer is the same.
     *
     * @param message message or builder
     * @throws IllegalStateException if the output differs
     */

    public void verify(MessageOrBuilder message) {
        Result result = compare(message);
        if (!result.isSame()) {
            throw new IllegalStateException(result.toString());
        }
    }

    private String writeBytes(MessageOrBuilder message) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (JsonGenerator generator = factory.createGenerator(ObjectWriteContext.empty(), output)) {
            writer.write(message, generator);
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    private String writeChars(MessageOrBuilder message) throws Exception {
        StringWriter output = new StringWriter();
        try (JsonGenerator generator = factory.createGenerator(ObjectWriteContext.empty(), output)) {
            writer.write(message, generator);
        }
        return output.toString();
    }

    // ---- JSON comparison, using jackson-core only ----

    private Object parse(String json, String path, List<String> differences) {
        try (JsonParser parser = factory.createParser(ObjectReadContext.empty(), json)) {
            parser.nextToken();
            return readValue(parser, path, differences);
        } catch (Exception e) {
            differences.add("invalid JSON: " + e.getMessage());
            return null;
        }
    }

    private Object readValue(JsonParser parser, String path, List<String> differences) {
        JsonToken token = parser.currentToken();
        switch (token) {
            case START_OBJECT -> {
                Map<String, Object> object = new LinkedHashMap<>();
                while (parser.nextToken() == JsonToken.PROPERTY_NAME) {
                    String name = parser.currentName();
                    parser.nextToken();
                    String childPath = path + "/" + name;
                    Object value = readValue(parser, childPath, differences);
                    if (object.containsKey(name)) {
                        differences.add(childPath + ": duplicate property");
                    }
                    object.put(name, value);
                }
                return object;
            }
            case START_ARRAY -> {
                List<Object> array = new ArrayList<>();
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    array.add(readValue(parser, path + "/" + array.size(), differences));
                }
                return array;
            }
            case VALUE_STRING -> {
                return parser.getString();
            }
            case VALUE_NUMBER_INT, VALUE_NUMBER_FLOAT -> {
                return parser.getDecimalValue();
            }
            case VALUE_TRUE -> {
                return Boolean.TRUE;
            }
            case VALUE_FALSE -> {
                return Boolean.FALSE;
            }
            case VALUE_NULL -> {
                return NULL;
            }
            default -> throw new IllegalStateException("Unexpected token " + token);
        }
    }

    @SuppressWarnings("unchecked")
    private static void diff(String path, Object expected, Object actual, List<String> differences) {
        if (differences.size() >= MAX_DIFFERENCES) {
            return;
        }
        if (expected instanceof Map && actual instanceof Map) {
            Map<String, Object> expectedObject = (Map<String, Object>) expected;
            Map<String, Object> actualObject = (Map<String, Object>) actual;
            Set<String> names = new LinkedHashSet<>(expectedObject.keySet());
            names.addAll(actualObject.keySet());
            for (String name : names) {
                String childPath = path + "/" + name;
                if (!expectedObject.containsKey(name)) {
                    differences.add(childPath + ": unexpected " + actualObject.get(name));
                } else if (!actualObject.containsKey(name)) {
                    differences.add(childPath + ": missing, expected " + expectedObject.get(name));
                } else {
                    diff(childPath, expectedObject.get(name), actualObject.get(name), differences);
                }
            }
        } else if (expected instanceof List && actual instanceof List) {
            List<Object> expectedArray = (List<Object>) expected;
            List<Object> actualArray = (List<Object>) actual;
            if (expectedArray.size() != actualArray.size()) {
                differences.add(path + ": expected " + expectedArray.size() + " elements, got " + actualArray.size());
                return;
            }
            for (int i = 0; i < expectedArray.size(); i++) {
                diff(path + "/" + i, expectedArray.get(i), actualArray.get(i), differences);
            }
        } else if (expected instanceof BigDecimal expectedNumber && actual instanceof BigDecimal actualNumber) {
            if (expectedNumber.compareTo(actualNumber) != 0) {
                differences.add(path + ": expected " + expectedNumber + ", got " + actualNumber);
            }
        } else if (!Objects.equals(expected, actual)) {
            differences.add(path + ": expected " + expected + ", got " + actual);
        }
    }
}
