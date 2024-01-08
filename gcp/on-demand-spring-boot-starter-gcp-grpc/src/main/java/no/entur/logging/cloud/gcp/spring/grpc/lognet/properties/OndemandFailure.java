package no.entur.logging.cloud.gcp.spring.grpc.lognet.properties;

public class OndemandFailure {

    private String level = "info";

    private OndemandGrpcResponseTrigger grpc = new OndemandGrpcResponseTrigger();
    private OndemandLogLevelTrigger logger = new OndemandLogLevelTrigger();

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLevel() {
        return level;
    }

    public OndemandGrpcResponseTrigger getGrpc() {
        return grpc;
    }

    public void setGrpc(OndemandGrpcResponseTrigger grpc) {
        this.grpc = grpc;
    }

    public OndemandLogLevelTrigger getLogger() {
        return logger;
    }

    public void setLogger(OndemandLogLevelTrigger logger) {
        this.logger = logger;
    }
}
