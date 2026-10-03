package no.entur.logging.cloud.protobuf.json;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Duration;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Int32Value;
import com.google.protobuf.ListValue;
import com.google.protobuf.Message;
import com.google.protobuf.NullValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf_test_messages.proto3.TestMessagesProto3.TestAllTypesProto3;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Fills messages with random (but valid) values, for comparing with JsonFormat.
 */

public class RandomMessages {

    public static JsonFormat.TypeRegistry createTypeRegistry() {
        return JsonFormat.TypeRegistry.newBuilder()
                .add(TestAllTypesProto3.getDescriptor())
                .add(List.of(Int32Value.getDescriptor(), StringValue.getDescriptor(), Timestamp.getDescriptor(), Duration.getDescriptor(),
                        Struct.getDescriptor(), Value.getDescriptor(), ListValue.getDescriptor(), FieldMask.getDescriptor(), Any.getDescriptor()))
                .build();
    }

    private static final int MAX_DEPTH = 3;

    private final Random random;

    public RandomMessages(long seed) {
        this.random = new Random(seed);
    }

    public <T extends Message> T fill(Message.Builder builder) {
        fill(builder, 0);
        return (T) builder.buildPartial();
    }

    private void fill(Message.Builder builder, int depth) {
        for (FieldDescriptor field : builder.getDescriptorForType().getFields()) {
            if (random.nextInt(3) == 0) {
                continue;
            }
            if (field.isMapField()) {
                int count = random.nextInt(4);
                Set<Object> keys = new HashSet<>();
                for (int i = 0; i < count; i++) {
                    Message.Builder entry = builder.newBuilderForField(field);
                    Descriptor entryType = field.getMessageType();
                    Object key = value(entryType.findFieldByNumber(1), entry, depth);
                    if (!keys.add(key)) {
                        continue;
                    }
                    entry.setField(entryType.findFieldByNumber(1), key);
                    if (random.nextInt(8) != 0) { // sometimes without value
                        entry.setField(entryType.findFieldByNumber(2), value(entryType.findFieldByNumber(2), entry, depth));
                    }
                    builder.addRepeatedField(field, entry.build());
                }
            } else if (field.isRepeated()) {
                int count = random.nextInt(4);
                for (int i = 0; i < count; i++) {
                    builder.addRepeatedField(field, value(field, builder, depth));
                }
            } else {
                builder.setField(field, value(field, builder, depth));
            }
        }
    }

    private Object value(FieldDescriptor field, Message.Builder parent, int depth) {
        return switch (field.getJavaType()) {
            case INT -> randomInt();
            case LONG -> randomLong();
            case FLOAT -> randomFloat();
            case DOUBLE -> randomDouble();
            case BOOLEAN -> random.nextBoolean();
            case STRING -> randomString();
            case BYTE_STRING -> randomBytes();
            case ENUM -> randomEnum(field);
            case MESSAGE -> message(field.getMessageType(), parent.newBuilderForField(field), depth + 1);
        };
    }

    private Object randomEnum(FieldDescriptor field) {
        EnumDescriptor type = field.getEnumType();
        if (!field.legacyEnumFieldTreatedAsClosed() && random.nextInt(10) == 0) {
            // unknown value of an open enum
            return type.findValueByNumberCreatingIfUnknown(1000 + random.nextInt(1000));
        }
        return type.getValues().get(random.nextInt(type.getValues().size()));
    }

    private Message message(Descriptor type, Message.Builder builder, int depth) {
        switch (type.getFullName()) {
            case "google.protobuf.Timestamp" -> builder.mergeFrom(Timestamp.newBuilder()
                    .setSeconds(random.nextLong(-62135596800L, 253402300799L))
                    .setNanos(randomNanos())
                    .build());
            case "google.protobuf.Duration" -> {
                long seconds = random.nextLong(-315576000000L, 315576000000L);
                int nanos = randomNanos();
                builder.mergeFrom(Duration.newBuilder().setSeconds(seconds).setNanos(seconds < 0 ? -nanos : nanos).build());
            }
            case "google.protobuf.FieldMask" -> {
                FieldMask.Builder mask = FieldMask.newBuilder();
                int count = random.nextInt(3);
                for (int i = 0; i < count; i++) {
                    mask.addPaths(random.nextBoolean() ? "foo_bar" : "a.b_c.d");
                }
                builder.mergeFrom(mask.build());
            }
            case "google.protobuf.Struct" -> builder.mergeFrom(randomStruct(depth));
            case "google.protobuf.Value" -> builder.mergeFrom(randomValue(depth));
            case "google.protobuf.ListValue" -> builder.mergeFrom(randomListValue(depth));
            case "google.protobuf.Any" -> {
                if (random.nextInt(4) == 0) {
                    builder.mergeFrom(Any.getDefaultInstance());
                } else if (random.nextBoolean() || depth > MAX_DEPTH) {
                    builder.mergeFrom(Any.pack(Int32Value.of(randomInt())));
                } else if (random.nextBoolean()) {
                    builder.mergeFrom(Any.pack(Timestamp.newBuilder().setSeconds(random.nextInt()).build()));
                } else {
                    TestAllTypesProto3 content = TestAllTypesProto3.newBuilder()
                            .setOptionalString(randomString())
                            .setOptionalInt64(randomLong())
                            .addRepeatedNestedEnum(TestAllTypesProto3.NestedEnum.BAZ)
                            .build();
                    builder.mergeFrom(Any.pack(content));
                }
            }
            default -> {
                if (depth <= MAX_DEPTH) {
                    fill(builder, depth);
                }
            }
        }
        return builder.buildPartial();
    }

    private int randomNanos() {
        return switch (random.nextInt(4)) {
            case 0 -> 0;
            case 1 -> random.nextInt(1000) * 1_000_000;
            case 2 -> random.nextInt(1_000_000) * 1_000;
            default -> random.nextInt(1_000_000_000);
        };
    }

    private Struct randomStruct(int depth) {
        Struct.Builder builder = Struct.newBuilder();
        int count = random.nextInt(4);
        for (int i = 0; i < count; i++) {
            builder.putFields(randomString(), randomValue(depth + 1));
        }
        return builder.build();
    }

    private ListValue randomListValue(int depth) {
        ListValue.Builder builder = ListValue.newBuilder();
        int count = random.nextInt(4);
        for (int i = 0; i < count; i++) {
            builder.addValues(randomValue(depth + 1));
        }
        return builder.build();
    }

    private Value randomValue(int depth) {
        int kind = random.nextInt(depth > MAX_DEPTH ? 4 : 7);
        return switch (kind) {
            case 0 -> Value.newBuilder().setNullValue(NullValue.NULL_VALUE).build();
            case 1 -> Value.newBuilder().setNumberValue(random.nextBoolean() ? random.nextInt(1000) : random.nextDouble() * 1e10).build();
            case 2 -> Value.newBuilder().setStringValue(randomString()).build();
            case 3 -> Value.newBuilder().setBoolValue(random.nextBoolean()).build();
            case 4 -> Value.newBuilder().setStructValue(randomStruct(depth)).build();
            case 5 -> Value.newBuilder().setListValue(randomListValue(depth)).build();
            default -> Value.getDefaultInstance(); // kind not set
        };
    }

    private int randomInt() {
        return switch (random.nextInt(5)) {
            case 0 -> 0;
            case 1 -> random.nextInt(128);
            case 2 -> Integer.MAX_VALUE;
            case 3 -> Integer.MIN_VALUE;
            default -> random.nextInt();
        };
    }

    private long randomLong() {
        return switch (random.nextInt(5)) {
            case 0 -> 0L;
            case 1 -> random.nextInt(128);
            case 2 -> Long.MAX_VALUE;
            case 3 -> Long.MIN_VALUE;
            default -> random.nextLong();
        };
    }

    private float randomFloat() {
        return switch (random.nextInt(8)) {
            case 0 -> Float.NaN;
            case 1 -> Float.POSITIVE_INFINITY;
            case 2 -> Float.NEGATIVE_INFINITY;
            case 3 -> Float.MAX_VALUE;
            case 4 -> Float.MIN_VALUE;
            case 5 -> -0.0f;
            default -> (random.nextFloat() - 0.5f) * random.nextInt(1_000_000);
        };
    }

    private double randomDouble() {
        return switch (random.nextInt(8)) {
            case 0 -> Double.NaN;
            case 1 -> Double.POSITIVE_INFINITY;
            case 2 -> Double.NEGATIVE_INFINITY;
            case 3 -> Double.MAX_VALUE;
            case 4 -> Double.MIN_VALUE;
            case 5 -> 1e-300;
            default -> (random.nextDouble() - 0.5) * random.nextInt(1_000_000);
        };
    }

    private static final String SPECIAL = "\"\\/\b\f\n\r\t\u0000\u0001\u001f\u007f<>&'=  ";

    private String randomString() {
        int length = random.nextInt(12);
        StringBuilder builder = new StringBuilder(length * 2);
        for (int i = 0; i < length; i++) {
            switch (random.nextInt(6)) {
                case 0 -> builder.append(SPECIAL.charAt(random.nextInt(SPECIAL.length())));
                case 1 -> builder.appendCodePoint(0x00C0 + random.nextInt(0x0100)); // latin
                case 2 -> builder.appendCodePoint(0x4E00 + random.nextInt(0x1000)); // CJK
                case 3 -> builder.appendCodePoint(0x1F600 + random.nextInt(0x40)); // emoji, surrogate pairs
                default -> builder.append((char) ('a' + random.nextInt(26)));
            }
        }
        return builder.toString();
    }

    private ByteString randomBytes() {
        byte[] bytes = new byte[random.nextInt(16)];
        random.nextBytes(bytes);
        return ByteString.copyFrom(bytes);
    }
}
