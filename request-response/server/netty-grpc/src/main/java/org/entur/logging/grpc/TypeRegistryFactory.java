package org.entur.logging.grpc;

import com.google.protobuf.Descriptors;
import com.google.protobuf.util.JsonFormat;
import com.google.rpc.BadRequest;
import com.google.rpc.DebugInfo;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Help;
import com.google.rpc.PreconditionFailure;
import com.google.rpc.QuotaFailure;
import com.google.rpc.ResourceInfo;
import com.google.rpc.RetryInfo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TypeRegistryFactory {

    private static final Set<Descriptors.Descriptor> DEFAULT_DESCRIPTORS = new HashSet<>(Arrays.asList(DebugInfo.getDescriptor(), ErrorInfo.getDescriptor(), PreconditionFailure.getDescriptor(), ResourceInfo.getDescriptor(), QuotaFailure.getDescriptor(), Help.getDescriptor(), RetryInfo.getDescriptor(), BadRequest.getDescriptor()));

    public static JsonFormat.TypeRegistry createDefaultTypeRegistry() {
        return createDefaultTypeRegistry(Collections.EMPTY_LIST);
    }

    public static JsonFormat.TypeRegistry createDefaultTypeRegistry(Collection<Descriptors.Descriptor> customDescriptors) {
        JsonFormat.TypeRegistry.Builder builder = JsonFormat.TypeRegistry.newBuilder();

        builder.add(DEFAULT_DESCRIPTORS);

        if (customDescriptors != null) {
            builder.add(customDescriptors);
        }

        return builder.build();
    }
}
