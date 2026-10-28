package no.entur.logging.cloud.rr.grpc.filter;

import no.entur.protobuf.json.AbstractProtobufJsonWriterBuilder;
import no.entur.protobuf.json.filter.JsonFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Filter applied while a gRPC payload is written as JSON (by protobuf-to-json):
 * <ul>
 *     <li>anonymize: keep the structure, but replace every scalar value with {@code "*"}</li>
 *     <li>prune: replace the whole value with {@code "[removed]"}</li>
 *     <li>max string length: truncate long string values</li>
 * </ul>
 * Paths refer to the JSON field names of the message, i.e. {@code $.customer.email}, {@code $.*.email} or
 * {@code $..email} (any depth). Keys are field names matched at any depth.
 * <br><br>
 * Immutable. Configure per service and/or method using {@linkplain GrpcServerLoggingFilters.Builder} or
 * {@linkplain GrpcClientLoggingFilters.Builder}.
 */

public final class GrpcPayloadFilter {

    public static final GrpcPayloadFilter NONE = newBuilder().build();

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private final List<String> anonymizePaths = new ArrayList<>();
        private final List<String> prunePaths = new ArrayList<>();
        private int maxStringLength = -1;

        public Builder withAnonymizePaths(String... paths) {
            return withAnonymizePaths(Arrays.asList(paths));
        }

        public Builder withAnonymizePaths(Collection<String> paths) {
            anonymizePaths.addAll(paths);
            return this;
        }

        public Builder withAnonymizeKeys(String... keys) {
            return withAnonymizeKeys(Arrays.asList(keys));
        }

        public Builder withAnonymizeKeys(Collection<String> keys) {
            for (String key : keys) {
                anonymizePaths.add("$.." + key);
            }
            return this;
        }

        public Builder withPrunePaths(String... paths) {
            return withPrunePaths(Arrays.asList(paths));
        }

        public Builder withPrunePaths(Collection<String> paths) {
            prunePaths.addAll(paths);
            return this;
        }

        public Builder withPruneKeys(String... keys) {
            return withPruneKeys(Arrays.asList(keys));
        }

        public Builder withPruneKeys(Collection<String> keys) {
            for (String key : keys) {
                prunePaths.add("$.." + key);
            }
            return this;
        }

        /**
         * Truncate text and bytes values longer than this.
         *
         * @param maxStringLength max length in characters, or -1 for no limit
         * @return this builder
         */

        public Builder withMaxStringLength(int maxStringLength) {
            this.maxStringLength = maxStringLength;
            return this;
        }

        /**
         * @return the filter
         * @throws IllegalArgumentException if a path is not valid, or the max string length is out of range
         */

        public GrpcPayloadFilter build() {
            // fail fast, rather than when the first message is written
            new JsonFilter(maxStringLength, -1, anonymizePaths, prunePaths, JsonFilter.DEFAULT_ANONYMIZE_RAW_JSON_VALUE, JsonFilter.DEFAULT_PRUNE_RAW_JSON_VALUE, JsonFilter.DEFAULT_TRUNCATE_MESSAGE);

            return new GrpcPayloadFilter(new ArrayList<>(anonymizePaths), new ArrayList<>(prunePaths), maxStringLength);
        }
    }

    private final List<String> anonymizePaths;
    private final List<String> prunePaths;
    private final int maxStringLength;

    private GrpcPayloadFilter(List<String> anonymizePaths, List<String> prunePaths, int maxStringLength) {
        this.anonymizePaths = Collections.unmodifiableList(anonymizePaths);
        this.prunePaths = Collections.unmodifiableList(prunePaths);
        this.maxStringLength = maxStringLength;
    }

    /**
     * Apply this filter to a protobuf-to-json writer builder.
     *
     * @param builder writer builder
     * @param <B>     builder type
     * @return the builder
     */

    public <B extends AbstractProtobufJsonWriterBuilder<B>> B configure(B builder) {
        if (!anonymizePaths.isEmpty()) {
            builder.withAnonymizePaths(anonymizePaths);
        }
        if (!prunePaths.isEmpty()) {
            builder.withPrunePaths(prunePaths);
        }
        if (maxStringLength != -1) {
            builder.withMaxStringLength(maxStringLength);
        }
        return builder;
    }

    public List<String> getAnonymizePaths() {
        return anonymizePaths;
    }

    public List<String> getPrunePaths() {
        return prunePaths;
    }

    public int getMaxStringLength() {
        return maxStringLength;
    }

    public boolean isNone() {
        return anonymizePaths.isEmpty() && prunePaths.isEmpty() && maxStringLength == -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GrpcPayloadFilter)) {
            return false;
        }
        GrpcPayloadFilter that = (GrpcPayloadFilter) o;
        return maxStringLength == that.maxStringLength && anonymizePaths.equals(that.anonymizePaths) && prunePaths.equals(that.prunePaths);
    }

    @Override
    public int hashCode() {
        return Objects.hash(anonymizePaths, prunePaths, maxStringLength);
    }

    @Override
    public String toString() {
        return "GrpcPayloadFilter{anonymizePaths=" + anonymizePaths + ", prunePaths=" + prunePaths + ", maxStringLength=" + maxStringLength + "}";
    }
}
