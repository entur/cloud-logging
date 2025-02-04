package no.entur.logging.cloud.spring.rr.grpc;

public class GrpcLoggingCloudProperties {

    protected int maxSize;

    protected int maxBodySize;

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getMaxBodySize() {
        return maxBodySize;
    }

    public void setMaxBodySize(int maxBodySize) {
        this.maxBodySize = maxBodySize;
    }

}
