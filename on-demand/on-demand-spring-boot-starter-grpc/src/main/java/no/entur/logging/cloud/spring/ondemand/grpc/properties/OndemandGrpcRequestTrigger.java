package no.entur.logging.cloud.spring.ondemand.grpc.properties;

import java.util.ArrayList;
import java.util.List;

public class OndemandGrpcRequestTrigger {

    private boolean enabled = true;

    private List<OndemandGrpcHeader> headers = new ArrayList<>();

    public List<OndemandGrpcHeader> getHeaders() {
        return headers;
    }

    public void setHeaders(List<OndemandGrpcHeader> headers) {
        this.headers = headers;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
