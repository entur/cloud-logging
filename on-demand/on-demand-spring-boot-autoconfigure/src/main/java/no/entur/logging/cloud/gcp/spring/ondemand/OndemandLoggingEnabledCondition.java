package no.entur.logging.cloud.gcp.spring.ondemand;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

public class OndemandLoggingEnabledCondition extends AnyNestedCondition {

    public OndemandLoggingEnabledCondition() {
        super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnProperty(name = {"entur.logging.http.ondemand.enabled"}, havingValue = "true", matchIfMissing = false)
    static class OnHttp {
    }

    @ConditionalOnProperty(name = {"entur.logging.http.grpc.enabled"}, havingValue = "true", matchIfMissing = false)
    static class OnGrpc {
    }

}