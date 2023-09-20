package no.entur.logging.cloud.rr.grpc.filter;

public class GrpcLogFilter {

    public static GrpcLogFilter FULL = GrpcLogFilter.newBuilder()
            .withConnect()
            .withRequest()
            .withResponse()
            .withDisconnect()
            .build();

    public static GrpcLogFilter REQUEST_RESPONSE = GrpcLogFilter.newBuilder()
            .withRequest()
            .withResponse()
            .build();

    public static GrpcLogFilter SUMMARY = GrpcLogFilter.newBuilder()
            .withDisconnect()
            .build();

    public static GrpcLogFilter NONE = GrpcLogFilter.newBuilder().build();

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        protected boolean connect;
        protected boolean disconnect;

        protected boolean request;
        protected boolean response;

        protected GrpcMetadataFilter requestMetadataFilter;
        protected GrpcMetadataFilter responseMetadataFilter;

        protected GrpcBodyFilter requestBodyFilter;
        protected GrpcBodyFilter responseBodyFilter;

        public Builder withConnect() {
            this.connect = true;
            return this;
        }

        public Builder withDisconnect() {
            this.disconnect = true;
            return this;
        }

        public Builder withRequest() {
            this.request = true;
            return this;
        }

        public Builder withResponse() {
            this.response = true;
            return this;
        }

        public Builder withRequestMetadataFilter(GrpcMetadataFilter requestMetadataFilter) {
            this.requestMetadataFilter = requestMetadataFilter;
            return this;
        }

        public Builder withResponseMetadataFilter(GrpcMetadataFilter responseMetadataFilter) {
            this.responseMetadataFilter = responseMetadataFilter;
            return this;
        }

        public Builder withRequestBodyFilter(GrpcBodyFilter requestBodyFilter) {
            this.requestBodyFilter = requestBodyFilter;
            return this;
        }

        public Builder withResponseBodyFilter(GrpcBodyFilter responseBodyFilter) {
            this.responseBodyFilter = responseBodyFilter;
            return this;
        }

        public GrpcLogFilter build() {
            if(requestMetadataFilter == null) {
                requestMetadataFilter = DefaultGrpcMetadataFilter.getInstance();
            }
            if(responseMetadataFilter == null) {
                responseMetadataFilter = NoneGrpcMetadataFilter.getInstance();
            }
            if(requestBodyFilter == null) {
                requestBodyFilter = NoneGrpcBodyFilter.getInstance();
            }
            if(responseBodyFilter == null) {
                responseBodyFilter = NoneGrpcBodyFilter.getInstance();
            }

            return new GrpcLogFilter(connect, disconnect, request, response, requestMetadataFilter, responseMetadataFilter, requestBodyFilter, responseBodyFilter);
        }
    }


    protected final boolean connect;
    protected final boolean disconnect;

    protected final boolean request;
    protected final boolean response;

    protected final GrpcMetadataFilter requestMetadataFilter;
    protected final GrpcMetadataFilter responseMetadataFilter;

    protected final GrpcBodyFilter requestBodyFilter;
    protected final GrpcBodyFilter responseBodyFilter;

    public GrpcLogFilter(boolean connect, boolean disconnect, boolean request, boolean response, GrpcMetadataFilter requestMetadataFilter, GrpcMetadataFilter responseMetadataFilter, GrpcBodyFilter requestBodyFilter, GrpcBodyFilter responseBodyFilter) {
        this.connect = connect;
        this.disconnect = disconnect;
        this.request = request;
        this.response = response;
        this.requestMetadataFilter = requestMetadataFilter;
        this.responseMetadataFilter = responseMetadataFilter;
        this.requestBodyFilter = requestBodyFilter;
        this.responseBodyFilter = responseBodyFilter;
    }

    public boolean isDisconnect() {
        return disconnect;
    }

    public boolean isConnect() {
        return connect;
    }

    public boolean isRequest() {
        return request;
    }

    public boolean isResponse() {
        return response;
    }

    public boolean isLogging() {
        return connect || disconnect || request || response;
    }

    public GrpcMetadataFilter getRequestMetadataFilter() {
        return requestMetadataFilter;
    }

    public GrpcMetadataFilter getResponseMetadataFilter() {
        return responseMetadataFilter;
    }

    public GrpcBodyFilter getRequestBodyFilter() {
        return requestBodyFilter;
    }

    public GrpcBodyFilter getResponseBodyFilter() {
        return responseBodyFilter;
    }
}
