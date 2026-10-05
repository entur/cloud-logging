package no.entur.logging.cloud.protobuf.json.plan;

import com.google.protobuf.Descriptors.Descriptor;

/**
 * Well-known types with a special JSON mapping, see
 * <a href="https://protobuf.dev/programming-guides/json/">ProtoJSON format</a>.
 */

public enum WellKnownType {

    ANY("google.protobuf.Any"),
    TIMESTAMP("google.protobuf.Timestamp"),
    DURATION("google.protobuf.Duration"),
    FIELD_MASK("google.protobuf.FieldMask"),
    STRUCT("google.protobuf.Struct"),
    VALUE("google.protobuf.Value"),
    LIST_VALUE("google.protobuf.ListValue"),
    BOOL_VALUE("google.protobuf.BoolValue"),
    INT32_VALUE("google.protobuf.Int32Value"),
    UINT32_VALUE("google.protobuf.UInt32Value"),
    INT64_VALUE("google.protobuf.Int64Value"),
    UINT64_VALUE("google.protobuf.UInt64Value"),
    STRING_VALUE("google.protobuf.StringValue"),
    BYTES_VALUE("google.protobuf.BytesValue"),
    FLOAT_VALUE("google.protobuf.FloatValue"),
    DOUBLE_VALUE("google.protobuf.DoubleValue");

    private final String fullName;

    WellKnownType(String fullName) {
        this.fullName = fullName;
    }

    /**
     * @param descriptor message type
     * @return well-known type, or null
     */

    public static WellKnownType of(Descriptor descriptor) {
        String name = descriptor.getFullName();
        if (!name.startsWith("google.protobuf.")) {
            return null;
        }
        for (WellKnownType type : values()) {
            if (type.fullName.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
