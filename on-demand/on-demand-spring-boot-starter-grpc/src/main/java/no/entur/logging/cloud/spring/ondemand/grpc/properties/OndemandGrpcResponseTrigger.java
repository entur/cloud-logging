package no.entur.logging.cloud.spring.ondemand.grpc.properties;

import io.grpc.Status;

import java.util.ArrayList;
import java.util.List;

public class OndemandGrpcResponseTrigger {

    private static List<String> allStatusCodesExceptOk() {
        ArrayList list = new ArrayList();
        Status.Code[] values = Status.Code.values();
        for (Status.Code value : values) {
            if(value != Status.Code.OK) {
                list.add(value.toString());
            }
        }
        return list;
    }

    private boolean enabled = true;

    private List<String> status = allStatusCodesExceptOk();

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getStatus() {
        return status;
    }

    public void setStatus(List<String> status) {
        this.status = status;
    }
}
