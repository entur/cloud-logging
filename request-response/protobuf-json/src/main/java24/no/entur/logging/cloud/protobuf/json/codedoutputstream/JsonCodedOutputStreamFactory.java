package no.entur.logging.cloud.protobuf.json.codedoutputstream;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.MessageLite;

import java.lang.classfile.ClassFile;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import static java.lang.constant.ConstantDescs.CD_void;
import static java.lang.constant.ConstantDescs.INIT_NAME;
import static java.lang.constant.ConstantDescs.MTD_void;

/**
 * Factory for {@linkplain CodedOutputStream}s which forward all writes to a {@linkplain CodedOutputStreamSink}, so that
 * the generated (reflection-free) serialization code of a message can drive JSON output.
 * <br><br>
 * {@linkplain CodedOutputStream} only has a private constructor. A subclass is therefore generated (using the
 * ClassFile API) and defined as a hidden class in {@linkplain CodedOutputStream}'s nest
 * ({@linkplain MethodHandles#privateLookupIn(Class, MethodHandles.Lookup)} and
 * {@linkplain MethodHandles.Lookup.ClassOption#NESTMATE}), which allows it to invoke the private constructor.
 * <br><br>
 * If the class cannot be defined (i.e. protobuf is loaded by a class loader which cannot see this class, or the
 * protobuf package is not open to this module), this feature is unavailable.
 * <br><br>
 * Java 24+ version (in the multi-release jar), using the ClassFile API.
 */

final class JsonCodedOutputStreamFactory {

    private static final MethodHandle CONSTRUCTOR;
    private static final Throwable UNAVAILABLE_CAUSE;

    static {
        MethodHandle constructor = null;
        Throwable cause = null;
        try {
            constructor = defineHiddenClass();

            // smoke test: resolves the sink interface from protobuf's class loader
            CodedOutputStream output = (CodedOutputStream) constructor.invokeExact((CodedOutputStreamSink) new NoopSink());
            output.writeInt32(1, 1);
        } catch (Throwable e) {
            constructor = null;
            cause = e;
        }
        CONSTRUCTOR = constructor;
        UNAVAILABLE_CAUSE = cause;
    }

    private JsonCodedOutputStreamFactory() {
    }

    static boolean isAvailable() {
        return CONSTRUCTOR != null;
    }

    /**
     * @return the reason why this feature is unavailable, or null
     */

    static Throwable getUnavailableCause() {
        return UNAVAILABLE_CAUSE;
    }

    static CodedOutputStream create(CodedOutputStreamSink sink) {
        try {
            return (CodedOutputStream) CONSTRUCTOR.invokeExact(sink);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    private static MethodHandle defineHiddenClass() throws ReflectiveOperationException {
        // all abstract methods must be implemented
        Set<String> required = new HashSet<>();
        for (Class<?> c = CodedOutputStream.class; c != Object.class; c = c.getSuperclass()) {
            for (Method method : c.getDeclaredMethods()) {
                if (Modifier.isAbstract(method.getModifiers())) {
                    required.add(method.getName() + toDescriptor(method));
                }
            }
        }

        ClassDesc self = ClassDesc.of(CodedOutputStream.class.getPackageName() + ".JsonCodedOutputStream");
        ClassDesc superclass = ClassDesc.of(CodedOutputStream.class.getName());
        ClassDesc sink = ClassDesc.of(CodedOutputStreamSink.class.getName());

        byte[] bytes = ClassFile.of().build(self, classBuilder -> {
            classBuilder.withSuperclass(superclass).withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL);
            classBuilder.withField("sink", sink, ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL);

            classBuilder.withMethodBody(INIT_NAME, MethodTypeDesc.of(CD_void, sink), ClassFile.ACC_PUBLIC, code -> code
                    .aload(0)
                    .invokespecial(superclass, INIT_NAME, MTD_void) // private constructor, accessible for nestmates
                    .aload(0)
                    .aload(1)
                    .putfield(self, "sink", sink)
                    .return_());

            // forward to the sink
            for (Method method : CodedOutputStreamSink.class.getMethods()) {
                String descriptor = toDescriptor(method);
                required.remove(method.getName() + descriptor);

                MethodTypeDesc methodType = MethodTypeDesc.ofDescriptor(descriptor);
                classBuilder.withMethodBody(method.getName(), methodType, ClassFile.ACC_PUBLIC, code -> {
                    code.aload(0).getfield(self, "sink", sink);
                    int slot = 1;
                    for (Class<?> parameterType : method.getParameterTypes()) {
                        TypeKind kind = TypeKind.from(parameterType);
                        code.loadLocal(kind, slot);
                        slot += kind.slotSize();
                    }
                    code.invokeinterface(sink, method.getName(), methodType);
                    code.return_(TypeKind.from(method.getReturnType()));
                });
            }
        });

        if (!required.isEmpty()) {
            throw new IllegalStateException("CodedOutputStreamSink does not implement " + required);
        }

        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(CodedOutputStream.class, MethodHandles.lookup());
        MethodHandles.Lookup hidden = lookup.defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE);
        return hidden.findConstructor(hidden.lookupClass(), MethodType.methodType(void.class, CodedOutputStreamSink.class))
                .asType(MethodType.methodType(CodedOutputStream.class, CodedOutputStreamSink.class));
    }

    private static String toDescriptor(Method method) {
        return MethodType.methodType(method.getReturnType(), method.getParameterTypes()).toMethodDescriptorString();
    }

    private static class NoopSink implements CodedOutputStreamSink {
        @Override public void writeTag(int fieldNumber, int wireType) {}
        @Override public void writeInt32(int fieldNumber, int value) {}
        @Override public void writeUInt32(int fieldNumber, int value) {}
        @Override public void writeFixed32(int fieldNumber, int value) {}
        @Override public void writeUInt64(int fieldNumber, long value) {}
        @Override public void writeFixed64(int fieldNumber, long value) {}
        @Override public void writeBool(int fieldNumber, boolean value) {}
        @Override public void writeString(int fieldNumber, String value) {}
        @Override public void writeBytes(int fieldNumber, ByteString value) {}
        @Override public void writeByteArray(int fieldNumber, byte[] value) {}
        @Override public void writeByteArray(int fieldNumber, byte[] value, int offset, int length) {}
        @Override public void writeByteBuffer(int fieldNumber, ByteBuffer value) {}
        @Override public void writeRawBytes(ByteBuffer value) {}
        @Override public void writeMessage(int fieldNumber, MessageLite value) {}
        @Override public void writeMessageSetExtension(int fieldNumber, MessageLite value) {}
        @Override public void writeRawMessageSetExtension(int fieldNumber, ByteString value) {}
        @Override public void writeInt32NoTag(int value) {}
        @Override public void writeUInt32NoTag(int value) {}
        @Override public void writeFixed32NoTag(int value) {}
        @Override public void writeUInt64NoTag(long value) {}
        @Override public void writeFixed64NoTag(long value) {}
        @Override public void writeStringNoTag(String value) {}
        @Override public void writeBytesNoTag(ByteString value) {}
        @Override public void writeMessageNoTag(MessageLite value) {}
        @Override public void writeByteArrayNoTag(byte[] value, int offset, int length) {}
        @Override public void write(byte value) {}
        @Override public void write(byte[] value, int offset, int length) {}
        @Override public void writeLazy(byte[] value, int offset, int length) {}
        @Override public void write(ByteBuffer value) {}
        @Override public void writeLazy(ByteBuffer value) {}
        @Override public void flush() {}
        @Override public int spaceLeft() { return Integer.MAX_VALUE; }
        @Override public int getTotalBytesWritten() { return 0; }
    }
}
