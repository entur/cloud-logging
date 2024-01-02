package no.entur.logging.cloud.trace.spring.grpc.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "entur.logging.grpc.trace.mdc")
public class GrpcMdcProperties {

    private boolean enabled = true;

    private boolean required = false;

    private boolean response = true;

    private int interceptorOrder;

    public int getInterceptorOrder() {
        return interceptorOrder;
    }

    public void setInterceptorOrder(int interceptorOrder) {
        this.interceptorOrder = interceptorOrder;
    }


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isResponse() {
        return response;
    }

    public void setResponse(boolean response) {
        this.response = response;
    }
}
