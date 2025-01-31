package no.entur.logging.cloud.spring.ondemand.grpc.properties;

public class OndemandFailure {

    private String level = "info";

    private OndemandGrpcResponseTrigger grpc = new OndemandGrpcResponseTrigger();
    private OndemandLogLevelTrigger logger = new OndemandLogLevelTrigger();
    private OndemandDurationTrigger duration = new OndemandDurationTrigger();

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

    public void setDuration(OndemandDurationTrigger duration) {
        this.duration = duration;
    }

    public OndemandDurationTrigger getDuration() {
        return duration;
    }
}
