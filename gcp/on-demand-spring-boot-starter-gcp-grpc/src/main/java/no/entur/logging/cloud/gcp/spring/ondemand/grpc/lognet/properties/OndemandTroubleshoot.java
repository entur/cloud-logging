package no.entur.logging.cloud.gcp.spring.ondemand.grpc.lognet.properties;

/**
 *
 * Troubleshooting only works for requests, i.e. debugging is assumed to hold so much data that
 * caching everything until the response is ready is too costly.
 *
 */

public class OndemandTroubleshoot {

    private String level = "debug";

    private OndemandGrpcRequestTrigger grpc = new OndemandGrpcRequestTrigger();

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLevel() {
        return level;
    }

    public OndemandGrpcRequestTrigger getGrpc() {
        return grpc;
    }

    public void setGrpc(OndemandGrpcRequestTrigger grpc) {
        this.grpc = grpc;
    }
}
