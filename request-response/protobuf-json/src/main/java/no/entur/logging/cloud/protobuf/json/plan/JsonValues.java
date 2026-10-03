package no.entur.logging.cloud.protobuf.json.plan;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.UnsafeByteOperations;
import no.entur.logging.cloud.protobuf.json.plan.MessagePlan.EnumNames;
import no.entur.logging.cloud.protobuf.json.plan.MessagePlan.FieldInfo;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.SerializableString;
import tools.jackson.core.json.UTF8JsonGenerator;
import tools.jackson.core.util.JsonGeneratorDelegate;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Write (scalar) field values from their wire format representation, using the
 * <a href="https://protobuf.dev/programming-guides/json/">ProtoJSON format</a>
 * (i.e. 64-bit integers as strings, unsigned integers as unsigned, bytes as base64).
 * <br><br>
 * Internal; shared by the JSON writer implementations.
 */

public final class JsonValues {

    private JsonValues() {
    }

    /**
     * Write a varint-encoded value.
     *
     * @param generator target
     * @param field field
     * @param raw value as on the wire (i.e. zigzag-encoded for sint32 and sint64)
     */

    public static void writeVarint(JsonGenerator generator, FieldInfo field, long raw) {
        switch (field.type) {
            case INT32 -> generator.writeNumber((int) raw);
            case UINT32 -> generator.writeNumber(raw & 0xFFFFFFFFL);
            case SINT32 -> generator.writeNumber(CodedInputStream.decodeZigZag32((int) raw));
            case INT64 -> generator.writeString(Long.toString(raw));
            case UINT64 -> generator.writeString(Long.toUnsignedString(raw));
            case SINT64 -> generator.writeString(Long.toString(CodedInputStream.decodeZigZag64(raw)));
            case BOOL -> generator.writeBoolean(raw != 0);
            case ENUM -> writeEnum(generator, field.enumNames, (int) raw);
            default -> throw new IllegalStateException("Unexpected varint field type " + field.type);
        }
    }

    public static void writeFixed32(JsonGenerator generator, FieldInfo field, int raw) {
        switch (field.type) {
            case FIXED32 -> generator.writeNumber(raw & 0xFFFFFFFFL);
            case SFIXED32 -> generator.writeNumber(raw);
            case FLOAT -> writeFloat(generator, Float.intBitsToFloat(raw));
            default -> throw new IllegalStateException("Unexpected fixed32 field type " + field.type);
        }
    }

    public static void writeFixed64(JsonGenerator generator, FieldInfo field, long raw) {
        switch (field.type) {
            case FIXED64 -> generator.writeString(Long.toUnsignedString(raw));
            case SFIXED64 -> generator.writeString(Long.toString(raw));
            case DOUBLE -> writeDouble(generator, Double.longBitsToDouble(raw));
            default -> throw new IllegalStateException("Unexpected fixed64 field type " + field.type);
        }
    }

    public static void writeFloat(JsonGenerator generator, float value) {
        if (Float.isNaN(value)) {
            generator.writeString("NaN");
        } else if (Float.isInfinite(value)) {
            generator.writeString(value > 0 ? "Infinity" : "-Infinity");
        } else {
            generator.writeNumber(value);
        }
    }

    public static void writeDouble(JsonGenerator generator, double value) {
        if (Double.isNaN(value)) {
            generator.writeString("NaN");
        } else if (Double.isInfinite(value)) {
            generator.writeString(value > 0 ? "Infinity" : "-Infinity");
        } else {
            generator.writeNumber(value);
        }
    }

    public static void writeEnum(JsonGenerator generator, EnumNames names, int number) {
        if (names.isNullValue()) {
            generator.writeNull();
            return;
        }
        SerializableString name = names.getName(number);
        if (name != null) {
            generator.writeString(name);
        } else {
            generator.writeNumber(number);
        }
    }

    /**
     * Check whether a generator writes bytes, so that it supports writing UTF-8 strings directly.
     *
     * @param generator generator
     * @return true if UTF-8 strings can be written directly
     */

    public static boolean isUtf8(JsonGenerator generator) {
        while (generator instanceof JsonGeneratorDelegate delegate) {
            generator = delegate.delegate();
        }
        return generator instanceof UTF8JsonGenerator;
    }

    /**
     * Write a string field from its UTF-8 bytes, without decoding if possible.
     */

    public static void writeUtf8(JsonGenerator generator, FieldInfo field, byte[] bytes, int offset, int length) {
        if (!isUtf8(generator) || (field.validateUtf8 && !UnsafeByteOperations.unsafeWrap(bytes, offset, length).isValidUtf8())) {
            // decoding replaces malformed input, like protobuf does when decoding to a String
            generator.writeString(new String(bytes, offset, length, StandardCharsets.UTF_8));
        } else {
            generator.writeUTF8String(bytes, offset, length);
        }
    }

    public static void writeBase64(JsonGenerator generator, byte[] bytes, int offset, int length) {
        ByteBuffer encoded = Base64.getEncoder().encode(ByteBuffer.wrap(bytes, offset, length));
        if (isUtf8(generator)) {
            // base64 never needs escaping
            generator.writeRawUTF8String(encoded.array(), encoded.arrayOffset() + encoded.position(), encoded.remaining());
        } else {
            generator.writeString(new String(encoded.array(), encoded.arrayOffset() + encoded.position(), encoded.remaining(), StandardCharsets.ISO_8859_1));
        }
    }

    // map keys are always strings

    public static String varintKey(FieldInfo key, long raw) {
        return switch (key.type) {
            case INT32 -> Integer.toString((int) raw);
            case UINT32 -> Long.toString(raw & 0xFFFFFFFFL);
            case SINT32 -> Integer.toString(CodedInputStream.decodeZigZag32((int) raw));
            case INT64 -> Long.toString(raw);
            case UINT64 -> Long.toUnsignedString(raw);
            case SINT64 -> Long.toString(CodedInputStream.decodeZigZag64(raw));
            case BOOL -> raw != 0 ? "true" : "false";
            default -> throw new IllegalStateException("Unexpected varint map key type " + key.type);
        };
    }

    public static String fixed32Key(FieldInfo key, int raw) {
        return switch (key.type) {
            case FIXED32 -> Long.toString(raw & 0xFFFFFFFFL);
            case SFIXED32 -> Integer.toString(raw);
            default -> throw new IllegalStateException("Unexpected fixed32 map key type " + key.type);
        };
    }

    public static String fixed64Key(FieldInfo key, long raw) {
        return switch (key.type) {
            case FIXED64 -> Long.toUnsignedString(raw);
            case SFIXED64 -> Long.toString(raw);
            default -> throw new IllegalStateException("Unexpected fixed64 map key type " + key.type);
        };
    }

    public static String defaultKey(FieldInfo key) {
        return switch (key.type) {
            case BOOL -> "false";
            case STRING -> "";
            default -> "0";
        };
    }
}
