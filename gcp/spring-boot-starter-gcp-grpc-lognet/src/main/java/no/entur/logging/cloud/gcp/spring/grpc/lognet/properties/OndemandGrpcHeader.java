package no.entur.logging.cloud.gcp.spring.grpc.lognet.properties;

public class OndemandGrpcHeader {

    private boolean enabled = true;
    private String name;

    public String getName() {
        return name;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
