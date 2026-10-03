package no.entur.logging.cloud.protobuf.json.transcode;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.BytesValue;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Duration;
import com.google.protobuf.FieldMask;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ListValue;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.UInt64Value;
import com.google.protobuf.Value;
import com.google.protobuf.WireFormat;
import com.google.protobuf.util.Durations;
import com.google.protobuf.util.FieldMaskUtil;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.Timestamps;
import no.entur.logging.cloud.protobuf.json.ProtobufJsonWriter;
import no.entur.logging.cloud.protobuf.json.plan.JsonValues;
import no.entur.logging.cloud.protobuf.json.plan.MessagePlan;
import no.entur.logging.cloud.protobuf.json.plan.MessagePlan.FieldInfo;
import no.entur.logging.cloud.protobuf.json.plan.MessagePlan.Plans;
import no.entur.logging.cloud.protobuf.json.plan.WellKnownType;
import tools.jackson.core.JsonGenerator;

import java.io.IOException;
import java.util.Map;

/**
 * {@linkplain ProtobufJsonWriter} which transcodes from the protobuf binary format: the message is serialized by its
 * (generated, reflection-free) code, then the bytes are read field by field and written as JSON.
 * <br><br>
 * Strings are written directly as UTF-8, without decoding (if the generator writes bytes). Unknown fields
 * (and extensions) are skipped, like JsonFormat does.
 * <br><br>
 * To never write duplicate JSON keys, fields must appear in increasing field number order, like protobuf
 * serializes them. A field which appears again after a higher field number (i.e. in the unknown fields)
 * is skipped.
 * <br><br>
 * Differences from JsonFormat:
 * <ul>
 *     <li>extensions are not written</li>
 *     <li>out of range timestamps and durations are written as objects rather than failing</li>
 *     <li>NaN and infinity values of google.protobuf.Value are written as strings rather than failing</li>
 * </ul>
 * Like JsonFormat, unresolvable google.protobuf.Any types fail.
 */

public class TranscodingProtobufJsonWriter implements ProtobufJsonWriter {

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private JsonFormat.TypeRegistry typeRegistry = JsonFormat.TypeRegistry.getEmptyTypeRegistry();

        /**
         * @param typeRegistry types for resolving google.protobuf.Any
         * @return this builder
         */

        public Builder withTypeRegistry(JsonFormat.TypeRegistry typeRegistry) {
            this.typeRegistry = typeRegistry;
            return this;
        }

        public TranscodingProtobufJsonWriter build() {
            return new TranscodingProtobufJsonWriter(new Plans(), typeRegistry);
        }
    }

    private static final byte[] EMPTY = new byte[0];

    private final Plans plans;
    private final JsonFormat.TypeRegistry typeRegistry;

    public TranscodingProtobufJsonWriter(Plans plans, JsonFormat.TypeRegistry typeRegistry) {
        this.plans = plans;
        this.typeRegistry = typeRegistry;
    }

    @Override
    public void write(MessageOrBuilder message, JsonGenerator generator) throws IOException {
        Message m;
        if (message instanceof Message msg) {
            m = msg;
        } else {
            m = ((Message.Builder) message).buildPartial();
        }
        write(m, generator);
    }

    public void write(Message message, JsonGenerator generator) throws IOException {
        MessagePlan plan = plans.get(message.getDescriptorForType());
        if (plan.wellKnownType != null) {
            writeWellKnown(plan.wellKnownType, message, generator);
        } else {
            byte[] bytes = message.toByteArray();
            writeMessage(bytes, 0, bytes.length, plan, generator, null);
        }
    }

    /**
     * Write a message as a JSON object, ignoring any well-known type mapping.
     *
     * @param typeUrl type url, for Any, or null
     */

    public void writeMessage(byte[] bytes, int offset, int length, MessagePlan plan, JsonGenerator generator, String typeUrl) throws IOException {
        CodedInputStream input = CodedInputStream.newInstance(bytes, offset, length);
        generator.writeStartObject();
        if (typeUrl != null) {
            generator.writeName("@type");
            generator.writeString(typeUrl);
        }
        writeFields(input, bytes, offset, plan, generator);
        generator.writeEndObject();
    }

    /**
     * Write fields until the end of the input (or the current limit), or until an end group tag.
     *
     * @param base offset of the input within the bytes
     */

    private void writeFields(CodedInputStream input, byte[] bytes, int base, MessagePlan plan, JsonGenerator generator) throws IOException {
        FieldInfo open = null;   // current repeated or map field
        boolean opened = false;  // whether the array or object has been started (unknown closed enum values are not written)
        int last = 0;            // last field number
        while (true) {
            int tag = input.readTag();
            if (tag == 0) {
                break;
            }
            int wireType = WireFormat.getTagWireType(tag);
            if (wireType == WireFormat.WIRETYPE_END_GROUP) {
                break;
            }
            int number = WireFormat.getTagFieldNumber(tag);

            FieldInfo field;
            if (open != null && open.number == number) {
                field = open;
            } else {
                if (open != null) {
                    if (opened) {
                        close(generator, open);
                    }
                    open = null;
                }
                field = plan.getField(number);
                if (field == null || number <= last) {
                    // unknown field, extension or out of order
                    input.skipField(tag);
                    continue;
                }
                last = number;
                if (field.repeated) {
                    open = field;
                    opened = false;
                }
            }
            if (!field.accepts(wireType)) {
                input.skipField(tag);
                continue;
            }

            if (field.map) {
                opened = writeMapEntry(input, bytes, base, field, generator, opened);
            } else if (field.repeated) {
                if (field.packable && wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    int limit = input.pushLimit(input.readRawVarint32());
                    while (input.getBytesUntilLimit() > 0) {
                        opened = writeElement(input, bytes, base, field, generator, opened);
                    }
                    input.popLimit(limit);
                } else {
                    opened = writeElement(input, bytes, base, field, generator, opened);
                }
            } else if (field.closedEnum) {
                int value = (int) input.readRawVarint64();
                if (isKnown(field, value)) {
                    generator.writeName(field.name);
                    JsonValues.writeEnum(generator, field.enumNames, value);
                }
            } else {
                generator.writeName(field.name);
                writeValue(input, bytes, base, field, generator);
            }
        }
        if (open != null && opened) {
            close(generator, open);
        }
    }

    private boolean writeElement(CodedInputStream input, byte[] bytes, int base, FieldInfo field, JsonGenerator generator, boolean opened) throws IOException {
        if (field.closedEnum) {
            int value = (int) input.readRawVarint64();
            if (isKnown(field, value)) {
                opened = open(generator, field, opened);
                JsonValues.writeEnum(generator, field.enumNames, value);
            }
            return opened;
        }
        opened = open(generator, field, opened);
        writeValue(input, bytes, base, field, generator);
        return opened;
    }

    private static boolean isKnown(FieldInfo field, int value) {
        return field.enumNames.getName(value) != null || field.enumNames.isNullValue();
    }

    private static boolean open(JsonGenerator generator, FieldInfo field, boolean opened) {
        if (!opened) {
            generator.writeName(field.name);
            if (field.map) {
                generator.writeStartObject();
            } else {
                generator.writeStartArray();
            }
        }
        return true;
    }

    private static void close(JsonGenerator generator, FieldInfo field) {
        if (field.map) {
            generator.writeEndObject();
        } else {
            generator.writeEndArray();
        }
    }

    /**
     * Write a single (non-packed) value. The field name is already written.
     */

    private void writeValue(CodedInputStream input, byte[] bytes, int base, FieldInfo field, JsonGenerator generator) throws IOException {
        switch (field.type) {
            case MESSAGE -> {
                int length = input.readRawVarint32();
                writeMessageValue(input, bytes, base, field.getMessagePlan(), length, generator);
            }
            case GROUP -> {
                generator.writeStartObject();
                writeFields(input, bytes, base, field.getMessagePlan(), generator);
                generator.writeEndObject();
            }
            case STRING -> {
                int length = input.readRawVarint32();
                JsonValues.writeUtf8(generator, field, bytes, base + input.getTotalBytesRead(), length);
                input.skipRawBytes(length);
            }
            case BYTES -> {
                int length = input.readRawVarint32();
                JsonValues.writeBase64(generator, bytes, base + input.getTotalBytesRead(), length);
                input.skipRawBytes(length);
            }
            case FIXED32, SFIXED32, FLOAT -> JsonValues.writeFixed32(generator, field, input.readRawLittleEndian32());
            case FIXED64, SFIXED64, DOUBLE -> JsonValues.writeFixed64(generator, field, input.readRawLittleEndian64());
            default -> JsonValues.writeVarint(generator, field, input.readRawVarint64());
        }
    }

    private void writeMessageValue(CodedInputStream input, byte[] bytes, int base, MessagePlan plan, int length, JsonGenerator generator) throws IOException {
        if (plan.wellKnownType != null) {
            writeWellKnown(plan.wellKnownType, bytes, base + input.getTotalBytesRead(), length, generator);
            input.skipRawBytes(length);
        } else {
            int limit = input.pushLimit(length);
            generator.writeStartObject();
            writeFields(input, bytes, base, plan, generator);
            generator.writeEndObject();
            input.popLimit(limit);
        }
    }

    /**
     * Write a map entry as a JSON object field. Map keys are always strings.
     *
     * @return whether the map object has been started
     */

    private boolean writeMapEntry(CodedInputStream input, byte[] bytes, int base, FieldInfo field, JsonGenerator generator, boolean opened) throws IOException {
        FieldInfo keyField = field.mapKey;
        FieldInfo valueField = field.mapValue;

        int limit = input.pushLimit(input.readRawVarint32());
        String key = null;
        boolean done = false;
        while (true) {
            int tag = input.readTag();
            if (tag == 0) {
                break;
            }
            int number = WireFormat.getTagFieldNumber(tag);
            int wireType = WireFormat.getTagWireType(tag);
            if (!done && number == 1 && keyField.accepts(wireType)) {
                key = readKey(input, keyField, wireType);
            } else if (!done && number == 2 && valueField.accepts(wireType)) {
                done = true;
                if (valueField.closedEnum) {
                    int value = (int) input.readRawVarint64();
                    // protobuf keeps entries with unknown closed enum values as unknown fields
                    if (isKnown(valueField, value)) {
                        opened = open(generator, field, opened);
                        generator.writeName(key != null ? key : JsonValues.defaultKey(keyField));
                        JsonValues.writeEnum(generator, valueField.enumNames, value);
                    }
                } else {
                    opened = open(generator, field, opened);
                    generator.writeName(key != null ? key : JsonValues.defaultKey(keyField));
                    writeValue(input, bytes, base, valueField, generator);
                }
            } else {
                input.skipField(tag);
            }
        }
        if (!done) {
            opened = open(generator, field, opened);
            generator.writeName(key != null ? key : JsonValues.defaultKey(keyField));
            writeDefault(valueField, generator);
        }
        input.popLimit(limit);
        return opened;
    }

    private static String readKey(CodedInputStream input, FieldInfo keyField, int wireType) throws IOException {
        return switch (wireType) {
            case WireFormat.WIRETYPE_VARINT -> JsonValues.varintKey(keyField, input.readRawVarint64());
            case WireFormat.WIRETYPE_FIXED32 -> JsonValues.fixed32Key(keyField, input.readRawLittleEndian32());
            case WireFormat.WIRETYPE_FIXED64 -> JsonValues.fixed64Key(keyField, input.readRawLittleEndian64());
            default -> input.readString();
        };
    }

    /**
     * Write the default value of a field, i.e. for map entries without value.
     */

    public void writeDefault(FieldInfo field, JsonGenerator generator) throws IOException {
        switch (field.type) {
            case MESSAGE, GROUP -> {
                MessagePlan plan = field.getMessagePlan();
                if (plan.wellKnownType != null) {
                    writeWellKnown(plan.wellKnownType, EMPTY, 0, 0, generator);
                } else {
                    generator.writeStartObject();
                    generator.writeEndObject();
                }
            }
            case STRING, BYTES -> generator.writeString("");
            case BOOL -> generator.writeBoolean(false);
            case ENUM -> JsonValues.writeEnum(generator, field.enumNames, ((EnumValueDescriptor) field.descriptor.getDefaultValue()).getNumber());
            case INT64, UINT64, SINT64, FIXED64, SFIXED64 -> generator.writeString("0");
            case FLOAT, DOUBLE -> generator.writeNumber(0.0d);
            default -> generator.writeNumber(0);
        }
    }

    // ---- well-known types ----

    public void writeWellKnown(WellKnownType type, byte[] bytes, int offset, int length, JsonGenerator generator) throws IOException {
        switch (type) {
            case ANY -> writeAny(Any.parser().parseFrom(bytes, offset, length), generator);
            case TIMESTAMP -> writeTimestamp(Timestamp.parser().parseFrom(bytes, offset, length), generator);
            case DURATION -> writeDuration(Duration.parser().parseFrom(bytes, offset, length), generator);
            case FIELD_MASK -> generator.writeString(FieldMaskUtil.toJsonString(FieldMask.parser().parseFrom(bytes, offset, length)));
            case STRUCT -> writeStruct(Struct.parser().parseFrom(bytes, offset, length), generator);
            case VALUE -> writeValue(Value.parser().parseFrom(bytes, offset, length), generator);
            case LIST_VALUE -> writeListValue(ListValue.parser().parseFrom(bytes, offset, length), generator);
            case BOOL_VALUE -> generator.writeBoolean(BoolValue.parser().parseFrom(bytes, offset, length).getValue());
            case INT32_VALUE -> generator.writeNumber(Int32Value.parser().parseFrom(bytes, offset, length).getValue());
            case UINT32_VALUE -> generator.writeNumber(Integer.toUnsignedLong(UInt32Value.parser().parseFrom(bytes, offset, length).getValue()));
            case INT64_VALUE -> generator.writeString(Long.toString(Int64Value.parser().parseFrom(bytes, offset, length).getValue()));
            case UINT64_VALUE -> generator.writeString(Long.toUnsignedString(UInt64Value.parser().parseFrom(bytes, offset, length).getValue()));
            case STRING_VALUE -> generator.writeString(StringValue.parser().parseFrom(bytes, offset, length).getValue());
            case BYTES_VALUE -> {
                byte[] value = BytesValue.parser().parseFrom(bytes, offset, length).getValue().toByteArray();
                JsonValues.writeBase64(generator, value, 0, value.length);
            }
            case FLOAT_VALUE -> JsonValues.writeFloat(generator, FloatValue.parser().parseFrom(bytes, offset, length).getValue());
            case DOUBLE_VALUE -> JsonValues.writeDouble(generator, DoubleValue.parser().parseFrom(bytes, offset, length).getValue());
        }
    }

    public void writeWellKnown(WellKnownType type, Message message, JsonGenerator generator) throws IOException {
        // use the generated classes directly if possible
        if (message instanceof Timestamp timestamp) {
            writeTimestamp(timestamp, generator);
        } else if (message instanceof Duration duration) {
            writeDuration(duration, generator);
        } else if (message instanceof Any any) {
            writeAny(any, generator);
        } else if (message instanceof Struct struct) {
            writeStruct(struct, generator);
        } else if (message instanceof Value value) {
            writeValue(value, generator);
        } else if (message instanceof ListValue listValue) {
            writeListValue(listValue, generator);
        } else {
            byte[] bytes = message.toByteArray();
            writeWellKnown(type, bytes, 0, bytes.length, generator);
        }
    }

    private void writeTimestamp(Timestamp timestamp, JsonGenerator generator) throws IOException {
        String value;
        try {
            value = Timestamps.toString(timestamp);
        } catch (IllegalArgumentException e) {
            // out of range; write the fields rather than fail (JsonFormat fails)
            byte[] bytes = timestamp.toByteArray();
            writeMessage(bytes, 0, bytes.length, plans.get(Timestamp.getDescriptor()), generator, null);
            return;
        }
        generator.writeString(value);
    }

    private void writeDuration(Duration duration, JsonGenerator generator) throws IOException {
        String value;
        try {
            value = Durations.toString(duration);
        } catch (IllegalArgumentException e) {
            // out of range; write the fields rather than fail (JsonFormat fails)
            byte[] bytes = duration.toByteArray();
            writeMessage(bytes, 0, bytes.length, plans.get(Duration.getDescriptor()), generator, null);
            return;
        }
        generator.writeString(value);
    }

    private void writeStruct(Struct struct, JsonGenerator generator) {
        generator.writeStartObject();
        for (Map.Entry<String, Value> entry : struct.getFieldsMap().entrySet()) {
            generator.writeName(entry.getKey());
            writeValue(entry.getValue(), generator);
        }
        generator.writeEndObject();
    }

    private void writeValue(Value value, JsonGenerator generator) {
        switch (value.getKindCase()) {
            // JsonFormat fails for NaN and infinity, write them as strings instead
            case NUMBER_VALUE -> JsonValues.writeDouble(generator, value.getNumberValue());
            case STRING_VALUE -> generator.writeString(value.getStringValue());
            case BOOL_VALUE -> generator.writeBoolean(value.getBoolValue());
            case STRUCT_VALUE -> writeStruct(value.getStructValue(), generator);
            case LIST_VALUE -> writeListValue(value.getListValue(), generator);
            default -> generator.writeNull(); // null value or kind not set
        }
    }

    private void writeListValue(ListValue listValue, JsonGenerator generator) {
        generator.writeStartArray();
        for (Value value : listValue.getValuesList()) {
            writeValue(value, generator);
        }
        generator.writeEndArray();
    }

    private void writeAny(Any any, JsonGenerator generator) throws IOException {
        String typeUrl = any.getTypeUrl();
        if (typeUrl.isEmpty() && any.getValue().isEmpty()) {
            generator.writeStartObject();
            generator.writeEndObject();
            return;
        }
        MessagePlan plan = plans.get(findDescriptor(typeUrl));
        byte[] content = any.getValue().toByteArray();
        if (plan.wellKnownType != null) {
            generator.writeStartObject();
            generator.writeName("@type");
            generator.writeString(typeUrl);
            generator.writeName("value");
            writeWellKnown(plan.wellKnownType, content, 0, content.length, generator);
            generator.writeEndObject();
        } else {
            writeMessage(content, 0, content.length, plan, generator, typeUrl);
        }
    }

    private Descriptor findDescriptor(String typeUrl) throws InvalidProtocolBufferException {
        // same as JsonFormat
        int index = typeUrl.lastIndexOf('/');
        if (index == -1) {
            throw new InvalidProtocolBufferException("Invalid type url found: " + typeUrl);
        }
        Descriptor descriptor = typeRegistry.find(typeUrl.substring(index + 1));
        if (descriptor == null) {
            throw new InvalidProtocolBufferException("Cannot find type for url: " + typeUrl);
        }
        return descriptor;
    }
}
