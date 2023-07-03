package org.entur.logging.grpc.filter;

public class GrpcLogFilter {

    public static GrpcLogFilter FULL = new GrpcLogFilter(true, true, true, true);
    public static GrpcLogFilter REQUEST_RESPONSE = new GrpcLogFilter(false, false, true, true);
    public static GrpcLogFilter SUMMARY = new GrpcLogFilter(false, true, false, false);
    public static GrpcLogFilter NONE = new GrpcLogFilter(false, false, false, false);

    public static GrpcLogFilter newInstance(boolean connect, boolean disconnect, boolean request, boolean response) {
        return new GrpcLogFilter(connect, disconnect, request, response);
    }

    protected final boolean connect;
    protected final boolean disconnect;

    protected final boolean request;
    protected final boolean response;

    public GrpcLogFilter(boolean connect, boolean disconnect, boolean request, boolean response) {
        this.connect = connect;
        this.disconnect = disconnect;
        this.request = request;
        this.response = response;
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
}
