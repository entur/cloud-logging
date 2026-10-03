package no.entur.logging.cloud.protobuf.json.plan;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.WireFormat;
import com.google.protobuf.util.JsonEnumValueOptions;
import com.google.protobuf.util.JsonEnumvalueOptionsProto;
import tools.jackson.core.SerializableString;
import tools.jackson.core.io.SerializedString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per message type information needed for writing JSON, looked up by field number.
 * Computed once per message type.
 * <br><br>
 * Internal; shared by the JSON writer implementations.
 */

public final class MessagePlan {

    private static final int MAX_DENSE_FIELD_NUMBER = 4096;

    /**
     * Cache of plans.
     */

    public static final class Plans {

        private final Map<Descriptor, MessagePlan> plans = new ConcurrentHashMap<>();

        public MessagePlan get(Descriptor descriptor) {
            MessagePlan plan = plans.get(descriptor);
            if (plan == null) {
                // note: not computeIfAbsent, as a plan does not resolve the plans of its fields' message types
                plan = new MessagePlan(descriptor, this);
                MessagePlan existing = plans.putIfAbsent(descriptor, plan);
                if (existing != null) {
                    plan = existing;
                }
            }
            return plan;
        }
    }

    public static final class EnumNames {

        private final boolean nullValue; // google.protobuf.NullValue is written as null
        private final SerializableString[] dense;
        private final Map<Integer, SerializableString> sparse;

        EnumNames(EnumDescriptor descriptor) {
            this.nullValue = descriptor.getFullName().equals("google.protobuf.NullValue");

            Map<Integer, SerializableString> names = new HashMap<>();
            int max = 0;
            boolean negative = false;
            for (EnumValueDescriptor value : descriptor.getValues()) {
                // first value wins for aliases, like in the generated code
                names.putIfAbsent(value.getNumber(), new SerializedString(getJsonName(value)));
                max = Math.max(max, value.getNumber());
                negative |= value.getNumber() < 0;
            }
            if (!negative && max <= MAX_DENSE_FIELD_NUMBER) {
                dense = new SerializableString[max + 1];
                for (Map.Entry<Integer, SerializableString> entry : names.entrySet()) {
                    dense[entry.getKey()] = entry.getValue();
                }
                sparse = null;
            } else {
                dense = null;
                sparse = names;
            }
        }

        private static String getJsonName(EnumValueDescriptor value) {
            JsonEnumValueOptions options = value.getOptions().getExtension(JsonEnumvalueOptionsProto.json);
            if (options.hasString()) {
                return options.getString();
            }
            return value.getName();
        }

        public boolean isNullValue() {
            return nullValue;
        }

        /**
         * @param number enum number
         * @return the name, or null if unknown
         */

        public SerializableString getName(int number) {
            if (dense != null) {
                if (number >= 0 && number < dense.length) {
                    return dense[number];
                }
                return null;
            }
            return sparse.get(number);
        }
    }

    public static final class FieldInfo {

        public final FieldDescriptor descriptor;
        public final int number;
        public final SerializableString name;
        public final FieldDescriptor.Type type;
        public final boolean repeated;
        public final boolean map;
        public final boolean packable;
        public final int wireType;

        /** strings which are not validated by protobuf must be validated before written as UTF-8 */
        public final boolean validateUtf8;

        /** unknown values of closed enums are kept as unknown fields by protobuf, so must not be written */
        public final boolean closedEnum;

        public final EnumNames enumNames;

        // for map fields
        public final FieldInfo mapKey;
        public final FieldInfo mapValue;

        private final Plans plans;
        private volatile MessagePlan messagePlan;

        FieldInfo(FieldDescriptor descriptor, Plans plans) {
            this.descriptor = descriptor;
            this.plans = plans;
            this.number = descriptor.getNumber();
            this.name = new SerializedString(descriptor.getJsonName());
            this.type = descriptor.getType();
            this.repeated = descriptor.isRepeated();
            this.map = descriptor.isMapField();
            this.packable = descriptor.isPackable();
            this.wireType = descriptor.getLiteType().getWireType();
            this.validateUtf8 = type == FieldDescriptor.Type.STRING && !descriptor.needsUtf8Check();

            if (type == FieldDescriptor.Type.ENUM) {
                this.enumNames = new EnumNames(descriptor.getEnumType());
                this.closedEnum = descriptor.legacyEnumFieldTreatedAsClosed();
            } else {
                this.enumNames = null;
                this.closedEnum = false;
            }

            if (map) {
                Descriptor entry = descriptor.getMessageType();
                this.mapKey = new FieldInfo(entry.findFieldByNumber(1), plans);
                this.mapValue = new FieldInfo(entry.findFieldByNumber(2), plans);
            } else {
                this.mapKey = null;
                this.mapValue = null;
            }
        }

        /**
         * Check whether a value with the given wire type can be written for this field.
         *
         * @param wireType wire type
         * @return true if acceptable
         */

        public boolean accepts(int wireType) {
            if (this.wireType == wireType) {
                return true;
            }
            // packed repeated scalars
            return repeated && packable && wireType == WireFormat.WIRETYPE_LENGTH_DELIMITED;
        }

        /**
         * @return plan for the message type of a message or group field
         */

        public MessagePlan getMessagePlan() {
            MessagePlan plan = this.messagePlan;
            if (plan == null) {
                plan = plans.get(descriptor.getMessageType());
                this.messagePlan = plan;
            }
            return plan;
        }
    }

    public final Descriptor descriptor;
    public final WellKnownType wellKnownType;

    private final FieldInfo[] dense;
    private final Map<Integer, FieldInfo> sparse;

    MessagePlan(Descriptor descriptor, Plans plans) {
        this.descriptor = descriptor;
        this.wellKnownType = WellKnownType.of(descriptor);

        List<FieldDescriptor> fields = descriptor.getFields();
        int max = 0;
        for (FieldDescriptor field : fields) {
            max = Math.max(max, field.getNumber());
        }
        if (max <= MAX_DENSE_FIELD_NUMBER) {
            dense = new FieldInfo[max + 1];
            for (FieldDescriptor field : fields) {
                dense[field.getNumber()] = new FieldInfo(field, plans);
            }
            sparse = null;
        } else {
            dense = null;
            sparse = new HashMap<>();
            for (FieldDescriptor field : fields) {
                sparse.put(field.getNumber(), new FieldInfo(field, plans));
            }
        }
    }

    /**
     * @param number field number
     * @return field, or null if unknown (including extensions)
     */

    public FieldInfo getField(int number) {
        if (dense != null) {
            if (number < dense.length) {
                return dense[number];
            }
            return null;
        }
        return sparse.get(number);
    }
}
