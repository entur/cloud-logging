package no.entur.logging.cloud.gcp.spring.grpc.lognet.properties;

public class OndemandFailure {

    private String level = "info";

    private OndemandGrpcTrigger grpc = new OndemandGrpcTrigger();
    private OndemandLogLevelTrigger logger = new OndemandLogLevelTrigger();

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLevel() {
        return level;
    }

    public OndemandGrpcTrigger getGrpc() {
        return grpc;
    }

    public void setGrpc(OndemandGrpcTrigger grpc) {
        this.grpc = grpc;
    }

    public OndemandLogLevelTrigger getLogger() {
        return logger;
    }

    public void setLogger(OndemandLogLevelTrigger logger) {
        this.logger = logger;
    }
}
