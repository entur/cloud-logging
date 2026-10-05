package no.entur.logging.cloud.protobuf.json.codedoutputstream;

import com.google.protobuf.CodedOutputStream;

import java.lang.classfile.ClassFile;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static java.lang.constant.ConstantDescs.CD_void;
import static java.lang.constant.ConstantDescs.INIT_NAME;
import static java.lang.constant.ConstantDescs.MTD_void;

/**
 * Generates a subclass of {@linkplain CodedOutputStream} which forwards all writes to a
 * {@linkplain CodedOutputStreamSink}, using the ClassFile API.
 * <br><br>
 * {@linkplain CodedOutputStream} only has a private constructor. The subclass is therefore defined as a hidden class
 * in {@linkplain CodedOutputStream}'s nest ({@linkplain MethodHandles#privateLookupIn(Class, MethodHandles.Lookup)}
 * and {@linkplain MethodHandles.Lookup.ClassOption#NESTMATE}), which allows it to invoke the private constructor.
 * <br><br>
 * Compiled for Java 24 (the rest of the module targets Java 17), so this class must only be loaded via
 * {@linkplain JsonCodedOutputStreamFactory}, which checks the Java version first.
 */

final class ClassFileCodedOutputStreamFactory {

    private ClassFileCodedOutputStreamFactory() {
    }

    /**
     * Generate a subclass of {@linkplain CodedOutputStream} which forwards all writes to a sink.
     *
     * @return the constructor, of type (CodedOutputStreamSink)CodedOutputStream
     */

    static MethodHandle defineConstructor() throws ReflectiveOperationException {
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
}
