package no.entur.logging.cloud.protobuf.json;

import com.google.protobuf.ApiProto;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Field;
import com.google.protobuf.Message;
import com.google.protobuf.SourceContext;
import com.google.protobuf.StructProto;
import com.google.protobuf.Syntax;
import com.google.protobuf.Timestamp;
import com.google.protobuf.TypeProto;
import com.google.protobuf_test_messages.proto3.TestMessagesProto3.TestAllTypesProto3;

import java.util.List;

public class BenchmarkPayloads {

    /**
     * @param name small, medium or large
     * @return message
     */

    public static Message create(String name) {
        return switch (name) {
            // typical small request
            case "small" -> TestAllTypesProto3.newBuilder()
                    .setOptionalString("Hello world")
                    .setOptionalInt32(42)
                    .setOptionalInt64(1234567890123L)
                    .setOptionalNestedEnum(TestAllTypesProto3.NestedEnum.BAR)
                    .setOptionalTimestamp(Timestamp.newBuilder().setSeconds(1700000000).build())
                    .setOptionalNestedMessage(TestAllTypesProto3.NestedMessage.newBuilder().setA(1))
                    .build();
            // all kinds of fields
            case "medium" -> new RandomMessages(42).fill(TestAllTypesProto3.newBuilder());
            // many strings, enums and nested messages
            case "large" -> createType();
            default -> throw new IllegalArgumentException(name);
        };
    }

    private static com.google.protobuf.Type createType() {
        com.google.protobuf.Type.Builder builder = com.google.protobuf.Type.newBuilder()
                .setName("big.Type")
                .setSyntax(Syntax.SYNTAX_PROTO3)
                .setSourceContext(SourceContext.newBuilder().setFileName("google/protobuf/descriptor.proto"));
        for (FileDescriptor file : List.of(DescriptorProtos.getDescriptor(), StructProto.getDescriptor(), TypeProto.getDescriptor(), ApiProto.getDescriptor())) {
            for (Descriptor descriptor : file.getMessageTypes()) {
                builder.addOneofs(descriptor.getFullName());
                for (FieldDescriptor field : descriptor.getFields()) {
                    builder.addFields(Field.newBuilder()
                            .setName(field.getName())
                            .setJsonName(field.getJsonName())
                            .setNumber(field.getNumber())
                            .setKind(Field.Kind.forNumber(field.getType().toProto().getNumber()))
                            .setCardinality(field.isRepeated() ? Field.Cardinality.CARDINALITY_REPEATED : Field.Cardinality.CARDINALITY_OPTIONAL)
                            .setTypeUrl("type.googleapis.com/" + descriptor.getFullName() + "." + field.getName())
                            .setPacked(field.isPacked()));
                }
            }
        }
        return builder.build();
    }
}
