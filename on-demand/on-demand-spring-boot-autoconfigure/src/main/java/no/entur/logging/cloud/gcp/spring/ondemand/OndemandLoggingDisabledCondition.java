package no.entur.logging.cloud.gcp.spring.ondemand;

import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

public class OndemandLoggingDisabledCondition extends AllNestedConditions {

    public OndemandLoggingDisabledCondition() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnProperty(name = {"entur.logging.http.ondemand.enabled"}, havingValue = "false", matchIfMissing = true)
    static class OnHttp {
    }

    @ConditionalOnProperty(name = {"entur.logging.http.grpc.enabled"}, havingValue = "false", matchIfMissing = true)
    static class OnGrpc {
    }

}