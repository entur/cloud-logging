package no.entur.logging.cloud.protobuf.json.codedoutputstream;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.WireFormat;
import no.entur.logging.cloud.protobuf.json.plan.JsonValues;
import no.entur.logging.cloud.protobuf.json.plan.MessagePlan;
import no.entur.logging.cloud.protobuf.json.plan.MessagePlan.FieldInfo;
import no.entur.logging.cloud.protobuf.json.plan.MessagePlan.Plans;
import no.entur.logging.cloud.protobuf.json.transcode.TranscodingProtobufJsonWriter;
import tools.jackson.core.JsonGenerator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Writes JSON by letting a message's generated serialization code write to a {@linkplain CodedOutputStream} which
 * produces JSON instead of the binary format. No intermediate binary representation is created.
 * <br><br>
 * Values arrive in one of these ways:
 * <ul>
 *     <li>tagged, i.e. writeInt32(fieldNumber, value) - generated code for regular fields</li>
 *     <li>a tag, then an untagged value, i.e. writeTag(fieldNumber, wireType) + writeInt32NoTag(value)
 *     - map entry keys and values, extensions</li>
 *     <li>packed: a length-delimited tag, the length in bytes, then untagged values - packed repeated fields</li>
 *     <li>map entries: a length-delimited tag, the length in bytes, then key and value as fields 1 and 2</li>
 *     <li>groups: start group tag, fields, end group tag</li>
 * </ul>
 * Some type information is lost on the way (i.e. sint64 values arrive zigzag-encoded via writeUInt64, floats as
 * raw bits via writeFixed32); the field descriptors are used to restore it.
 * <br><br>
 * Like {@linkplain TranscodingProtobufJsonWriter}, fields are expected in increasing field number order, unknown fields
 * and extensions are skipped, and so are unknown closed enum values.
 * <br><br>
 * Not thread-safe; create one instance per message.
 */

final class CodedOutputStreamJsonSink implements CodedOutputStreamSink {

    private static final class Frame {
        MessagePlan plan;
        FieldInfo open;             // current repeated or map field
        boolean opened;             // whether the array or object has been started
        int last;                   // last field number
        Set<String> keys;           // keys of the current map field

        // tag written, value to follow
        int pendingNumber;
        int pendingWireType;

        // packed field
        boolean expectPackedLength;
        FieldInfo packed;           // null if skipped
        int packedRemaining;        // remaining bytes

        // map entry
        FieldInfo entry;
        String key;
        boolean entryDone;
        boolean entryTagged;        // entry written as tag, length, key and value (rather than as a message)
        boolean expectEntryLength;

        void reset(MessagePlan plan, FieldInfo entry) {
            this.plan = plan;
            this.open = null;
            this.opened = false;
            this.last = 0;
            this.pendingNumber = 0;
            this.pendingWireType = 0;
            this.expectPackedLength = false;
            this.packed = null;
            this.packedRemaining = 0;
            this.entry = entry;
            this.key = null;
            this.entryDone = false;
            this.entryTagged = false;
            this.expectEntryLength = false;
        }
    }

    private final Plans plans;
    private final TranscodingProtobufJsonWriter transcoder;
    private final JsonGenerator generator;
    private final CodedOutputStream output;

    private Frame[] frames = new Frame[8];
    private int depth = -1;
    private Frame frame;

    private int skipGroups; // > 0 while within a skipped (i.e. unknown) group

    CodedOutputStreamJsonSink(Plans plans, TranscodingProtobufJsonWriter transcoder, JsonGenerator generator) {
        this.plans = plans;
        this.transcoder = transcoder;
        this.generator = generator;
        this.output = JsonCodedOutputStreamFactory.create(this);
    }

    void write(Message message) throws IOException {
        push(plans.get(message.getDescriptorForType()), null);
        writeObject(message);
        pop();
    }

    /**
     * Write a message as an object, in the current frame.
     */

    private void writeObject(MessageLite message) throws IOException {
        generator.writeStartObject();
        message.writeTo(output);
        finishTaggedEntry();
        closeOpen();
        generator.writeEndObject();
    }

    // ---- frames ----

    private void push(MessagePlan plan, FieldInfo entry) {
        depth++;
        if (depth == frames.length) {
            frames = Arrays.copyOf(frames, frames.length * 2);
        }
        Frame f = frames[depth];
        if (f == null) {
            frames[depth] = f = new Frame();
        }
        f.reset(plan, entry);
        frame = f;
    }

    private void pop() {
        depth--;
        frame = depth >= 0 ? frames[depth] : null;
    }

    private void closeOpen() {
        Frame f = frame;
        if (f.open != null) {
            if (f.opened) {
                if (f.open.map) {
                    generator.writeEndObject();
                } else {
                    generator.writeEndArray();
                }
            }
            f.open = null;
        }
    }

    /**
     * Select a field for writing a value with the given wire type.
     *
     * @return the field, or null if the value should be skipped
     */

    private FieldInfo select(int number, int wireType) {
        Frame f = frame;
        FieldInfo field;
        if (f.open != null && f.open.number == number) {
            field = f.open;
        } else {
            closeOpen();
            field = f.plan.getField(number);
            if (field == null || number <= f.last) {
                // unknown field, extension or out of order
                return null;
            }
            f.last = number;
            if (field.repeated) {
                f.open = field;
                f.opened = false;
                if (field.map) {
                    if (f.keys == null) {
                        f.keys = new HashSet<>();
                    } else {
                        f.keys.clear();
                    }
                }
            }
        }
        if (!field.accepts(wireType)) {
            return null;
        }
        return field;
    }

    /**
     * Write the field name, or start the array or object of a repeated or map field.
     */

    private void begin(FieldInfo field) {
        if (field.repeated) {
            Frame f = frame;
            if (!f.opened) {
                generator.writeName(field.name);
                if (field.map) {
                    generator.writeStartObject();
                } else {
                    generator.writeStartArray();
                }
                f.opened = true;
            }
        } else {
            generator.writeName(field.name);
        }
    }

    private static boolean isKnown(FieldInfo field, long value) {
        return field.enumNames.getName((int) value) != null || field.enumNames.isNullValue();
    }

    // ---- map entries ----

    /**
     * Write the name of a map entry, unless an entry with the same key was already written, so that no JSON key is
     * written twice (generated messages never have duplicate keys).
     *
     * @return true if the name was written and the value should follow
     */

    private boolean writeEntryName() {
        Frame f = frame;
        String name = f.key != null ? f.key : JsonValues.defaultKey(f.entry.mapKey);
        // the keys are tracked by the frame of the message with the map field
        if (!frames[depth - 1].keys.add(name)) {
            return false;
        }
        generator.writeName(name);
        return true;
    }

    /**
     * Called after a map entry value was handled. Entries written as tag + length + key + value always end with
     * the value (see MapEntryLite), so the entry is then complete.
     */

    private void entryValueDone() {
        Frame f = frame;
        f.entryDone = true;
        if (f.entryTagged) {
            pop();
        }
    }

    /**
     * Complete a map entry without value (written as tag + length + key + value).
     */

    private void finishTaggedEntry() throws IOException {
        Frame f = frame;
        if (f.entry != null && f.entryTagged) {
            if (!f.entryDone) {
                if (writeEntryName()) {
                    transcoder.writeDefault(f.entry.mapValue, generator);
                }
            }
            pop();
        }
    }

    // ---- tagged values ----

    private void varint(int number, long value) {
        if (skipGroups > 0) {
            return;
        }
        Frame f = frame;
        if (f.entry != null) {
            if (number == 1 && f.entry.mapKey.accepts(WireFormat.WIRETYPE_VARINT)) {
                f.key = JsonValues.varintKey(f.entry.mapKey, value);
            } else if (number == 2 && !f.entryDone && f.entry.mapValue.accepts(WireFormat.WIRETYPE_VARINT)) {
                FieldInfo valueField = f.entry.mapValue;
                // protobuf keeps entries with unknown closed enum values as unknown fields
                if (!valueField.closedEnum || isKnown(valueField, value)) {
                    if (writeEntryName()) {
                        JsonValues.writeVarint(generator, valueField, value);
                    }
                }
                entryValueDone();
            }
            return;
        }
        FieldInfo field = select(number, WireFormat.WIRETYPE_VARINT);
        if (field == null || (field.closedEnum && !isKnown(field, value))) {
            return;
        }
        begin(field);
        JsonValues.writeVarint(generator, field, value);
    }

    @Override
    public void writeInt32(int fieldNumber, int value) {
        varint(fieldNumber, value);
    }

    @Override
    public void writeUInt32(int fieldNumber, int value) {
        varint(fieldNumber, value & 0xFFFFFFFFL);
    }

    @Override
    public void writeUInt64(int fieldNumber, long value) {
        varint(fieldNumber, value);
    }

    @Override
    public void writeBool(int fieldNumber, boolean value) {
        varint(fieldNumber, value ? 1 : 0);
    }

    @Override
    public void writeFixed32(int fieldNumber, int value) {
        if (skipGroups > 0) {
            return;
        }
        Frame f = frame;
        if (f.entry != null) {
            if (fieldNumber == 1 && f.entry.mapKey.accepts(WireFormat.WIRETYPE_FIXED32)) {
                f.key = JsonValues.fixed32Key(f.entry.mapKey, value);
            } else if (fieldNumber == 2 && !f.entryDone && f.entry.mapValue.accepts(WireFormat.WIRETYPE_FIXED32)) {
                if (writeEntryName()) {
                    JsonValues.writeFixed32(generator, f.entry.mapValue, value);
                }
                entryValueDone();
            }
            return;
        }
        FieldInfo field = select(fieldNumber, WireFormat.WIRETYPE_FIXED32);
        if (field != null) {
            begin(field);
            JsonValues.writeFixed32(generator, field, value);
        }
    }

    @Override
    public void writeFixed64(int fieldNumber, long value) {
        if (skipGroups > 0) {
            return;
        }
        Frame f = frame;
        if (f.entry != null) {
            if (fieldNumber == 1 && f.entry.mapKey.accepts(WireFormat.WIRETYPE_FIXED64)) {
                f.key = JsonValues.fixed64Key(f.entry.mapKey, value);
            } else if (fieldNumber == 2 && !f.entryDone && f.entry.mapValue.accepts(WireFormat.WIRETYPE_FIXED64)) {
                if (writeEntryName()) {
                    JsonValues.writeFixed64(generator, f.entry.mapValue, value);
                }
                entryValueDone();
            }
            return;
        }
        FieldInfo field = select(fieldNumber, WireFormat.WIRETYPE_FIXED64);
        if (field != null) {
            begin(field);
            JsonValues.writeFixed64(generator, field, value);
        }
    }

    @Override
    public void writeString(int fieldNumber, String value) {
        if (skipGroups > 0) {
            return;
        }
        Frame f = frame;
        if (f.entry != null) {
            if (fieldNumber == 1 && f.entry.mapKey.type == Type.STRING) {
                f.key = value;
            } else if (fieldNumber == 2 && !f.entryDone && f.entry.mapValue.type == Type.STRING) {
                if (writeEntryName()) {
                    generator.writeString(value);
                }
                entryValueDone();
            }
            return;
        }
        FieldInfo field = select(fieldNumber, WireFormat.WIRETYPE_LENGTH_DELIMITED);
        if (field != null && field.type == Type.STRING) {
            begin(field);
            generator.writeString(value);
        }
    }

    @Override
    public void writeBytes(int fieldNumber, ByteString value) {
        writeBytes(fieldNumber, value, null, 0, 0);
    }

    @Override
    public void writeByteArray(int fieldNumber, byte[] value) {
        writeBytes(fieldNumber, null, value, 0, value.length);
    }

    @Override
    public void writeByteArray(int fieldNumber, byte[] value, int offset, int length) {
        writeBytes(fieldNumber, null, value, offset, length);
    }

    @Override
    public void writeByteBuffer(int fieldNumber, ByteBuffer value) {
        byte[] bytes = new byte[value.remaining()];
        value.duplicate().get(bytes);
        writeBytes(fieldNumber, null, bytes, 0, bytes.length);
    }

    /**
     * Write a string or bytes value, given either as a ByteString or a byte array.
     */

    private void writeBytes(int fieldNumber, ByteString byteString, byte[] bytes, int offset, int length) {
        if (skipGroups > 0) {
            return;
        }
        Frame f = frame;
        if (f.entry != null) {
            if (fieldNumber == 1 && f.entry.mapKey.type == Type.STRING) {
                f.key = byteString != null ? byteString.toStringUtf8() : new String(bytes, offset, length, StandardCharsets.UTF_8);
            } else if (fieldNumber == 2 && !f.entryDone) {
                FieldInfo valueField = f.entry.mapValue;
                if (valueField.type == Type.STRING || valueField.type == Type.BYTES) {
                    if (writeEntryName()) {
                        writeBytesValue(valueField, byteString, bytes, offset, length);
                    }
                    entryValueDone();
                }
            }
            return;
        }
        FieldInfo field = select(fieldNumber, WireFormat.WIRETYPE_LENGTH_DELIMITED);
        if (field != null && (field.type == Type.STRING || field.type == Type.BYTES)) {
            begin(field);
            writeBytesValue(field, byteString, bytes, offset, length);
        }
    }

    private void writeBytesValue(FieldInfo field, ByteString byteString, byte[] bytes, int offset, int length) {
        if (byteString != null) {
            bytes = byteString.toByteArray();
            offset = 0;
            length = bytes.length;
        }
        if (field.type == Type.STRING) {
            // not yet decoded string
            JsonValues.writeUtf8(generator, field, bytes, offset, length);
        } else {
            JsonValues.writeBase64(generator, bytes, offset, length);
        }
    }

    @Override
    public void writeMessage(int fieldNumber, MessageLite value) throws IOException {
        if (skipGroups > 0) {
            return;
        }
        Frame f = frame;
        if (f.entry != null) {
            FieldInfo valueField = f.entry.mapValue;
            if (fieldNumber == 2 && !f.entryDone && valueField.type == Type.MESSAGE) {
                if (writeEntryName()) {
                    writeMessageValue(valueField, value);
                }
                entryValueDone();
            }
            return;
        }
        FieldInfo field = select(fieldNumber, WireFormat.WIRETYPE_LENGTH_DELIMITED);
        if (field == null) {
            return;
        }
        if (field.map) {
            // map entry as a message
            begin(field);
            push(null, field);
            value.writeTo(output);
            if (!frame.entryDone) {
                if (writeEntryName()) {
                    transcoder.writeDefault(field.mapValue, generator);
                }
            }
            pop();
        } else if (field.type == Type.MESSAGE) {
            begin(field);
            writeMessageValue(field, value);
        }
    }

    private void writeMessageValue(FieldInfo field, MessageLite value) throws IOException {
        MessagePlan plan = field.getMessagePlan();
        if (plan.wellKnownType != null) {
            if (value instanceof Message message) {
                transcoder.writeWellKnown(plan.wellKnownType, message, generator);
            } else {
                byte[] bytes = value.toByteArray();
                transcoder.writeWellKnown(plan.wellKnownType, bytes, 0, bytes.length, generator);
            }
        } else {
            push(plan, null);
            writeObject(value);
            pop();
        }
    }

    // ---- tags ----

    @Override
    public void writeTag(int fieldNumber, int wireType) {
        switch (wireType) {
            case WireFormat.WIRETYPE_START_GROUP -> startGroup(fieldNumber);
            case WireFormat.WIRETYPE_END_GROUP -> endGroup();
            default -> {
                if (skipGroups == 0) {
                    // value to follow
                    Frame f = frame;
                    f.pendingNumber = fieldNumber;
                    f.pendingWireType = wireType;
                }
            }
        }
    }

    private void startGroup(int fieldNumber) {
        if (skipGroups > 0 || frame.entry != null) {
            skipGroups++;
            return;
        }
        FieldInfo field = select(fieldNumber, WireFormat.WIRETYPE_START_GROUP);
        if (field == null) {
            skipGroups = 1;
            return;
        }
        begin(field);
        push(field.getMessagePlan(), null);
        generator.writeStartObject();
    }

    private void endGroup() {
        if (skipGroups > 0) {
            skipGroups--;
            return;
        }
        closeOpen();
        generator.writeEndObject();
        pop();
    }

    /**
     * A length-delimited tag followed by a length: a map entry or a packed field.
     */

    private void startLengthDelimited(int fieldNumber) {
        Frame f = frame;
        FieldInfo field = f.entry == null ? select(fieldNumber, WireFormat.WIRETYPE_LENGTH_DELIMITED) : null;
        if (field != null && field.map) {
            // map entry, written as tag, length, key and value
            begin(field);
            push(null, field);
            frame.entryTagged = true;
            frame.expectEntryLength = true;
        } else {
            f.packed = field != null && field.packable ? field : null;
            f.expectPackedLength = true;
        }
    }

    // ---- untagged values ----

    @Override
    public void writeUInt32NoTag(int value) throws IOException {
        if (skipGroups > 0) {
            return;
        }
        Frame f = frame;
        if (f.expectEntryLength) {
            f.expectEntryLength = false;
        } else if (f.packedRemaining > 0) {
            packedVarint(value & 0xFFFFFFFFL, CodedOutputStream.computeUInt32SizeNoTag(value));
        } else if (f.expectPackedLength) {
            f.expectPackedLength = false;
            f.packedRemaining = value;
        } else if (f.pendingNumber != 0) {
            int wireType = f.pendingWireType;
            int number = takePending();
            if (wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                startLengthDelimited(number);
                writeUInt32NoTag(value); // the length
            } else {
                varint(number, value & 0xFFFFFFFFL);
            }
        } else if (WireFormat.getTagWireType(value) == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
            // tag of a packed field or map entry, written by generated code
            startLengthDelimited(WireFormat.getTagFieldNumber(value));
        }
    }

    private int takePending() {
        Frame f = frame;
        int number = f.pendingNumber;
        f.pendingNumber = 0;
        return number;
    }

    @Override
    public void writeInt32NoTag(int value) {
        if (skipGroups > 0) {
            return;
        }
        Frame f = frame;
        if (f.packedRemaining > 0) {
            packedVarint(value, CodedOutputStream.computeInt32SizeNoTag(value));
        } else if (f.pendingNumber != 0) {
            varint(takePending(), value);
        }
    }

    @Override
    public void writeUInt64NoTag(long value) {
        if (skipGroups > 0) {
            return;
        }
        Frame f = frame;
        if (f.packedRemaining > 0) {
            packedVarint(value, CodedOutputStream.computeUInt64SizeNoTag(value));
        } else if (f.pendingNumber != 0) {
            varint(takePending(), value);
        }
    }

    @Override
    public void write(byte value) {
        // writeBoolNoTag
        if (skipGroups > 0) {
            return;
        }
        Frame f = frame;
        if (f.packedRemaining > 0) {
            packedVarint(value, 1);
        } else if (f.pendingNumber != 0) {
            varint(takePending(), value);
        }
    }

    private void packedVarint(long value, int size) {
        Frame f = frame;
        f.packedRemaining -= size;
        FieldInfo field = f.packed;
        if (field == null || (field.closedEnum && !isKnown(field, value))) {
            return;
        }
        begin(field);
        JsonValues.writeVarint(generator, field, value);
    }

    @Override
    public void writeFixed32NoTag(int value) {
        if (skipGroups > 0) {
            return;
        }
        Frame f = frame;
        if (f.packedRemaining > 0) {
            f.packedRemaining -= 4;
            if (f.packed != null) {
                begin(f.packed);
                JsonValues.writeFixed32(generator, f.packed, value);
            }
        } else if (f.pendingNumber != 0) {
            writeFixed32(takePending(), value);
        }
    }

    @Override
    public void writeFixed64NoTag(long value) {
        if (skipGroups > 0) {
            return;
        }
        Frame f = frame;
        if (f.packedRemaining > 0) {
            f.packedRemaining -= 8;
            if (f.packed != null) {
                begin(f.packed);
                JsonValues.writeFixed64(generator, f.packed, value);
            }
        } else if (f.pendingNumber != 0) {
            writeFixed64(takePending(), value);
        }
    }

    @Override
    public void writeStringNoTag(String value) {
        if (skipGroups == 0 && frame.pendingNumber != 0) {
            writeString(takePending(), value);
        }
    }

    @Override
    public void writeBytesNoTag(ByteString value) {
        if (skipGroups == 0 && frame.pendingNumber != 0) {
            writeBytes(takePending(), value);
        }
    }

    @Override
    public void writeByteArrayNoTag(byte[] value, int offset, int length) {
        if (skipGroups == 0 && frame.pendingNumber != 0) {
            writeByteArray(takePending(), value, offset, length);
        }
    }

    @Override
    public void writeMessageNoTag(MessageLite value) throws IOException {
        if (skipGroups == 0 && frame.pendingNumber != 0) {
            writeMessage(takePending(), value);
        }
    }

    // ---- not used by generated code for regular fields ----

    @Override
    public void writeRawBytes(ByteBuffer value) {
    }

    @Override
    public void writeMessageSetExtension(int fieldNumber, MessageLite value) {
    }

    @Override
    public void writeRawMessageSetExtension(int fieldNumber, ByteString value) {
    }

    @Override
    public void write(byte[] value, int offset, int length) {
    }

    @Override
    public void writeLazy(byte[] value, int offset, int length) {
    }

    @Override
    public void write(ByteBuffer value) {
    }

    @Override
    public void writeLazy(ByteBuffer value) {
    }

    @Override
    public void flush() {
    }

    @Override
    public int spaceLeft() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getTotalBytesWritten() {
        return 0;
    }
}
