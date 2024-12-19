package no.entur.logging.cloud.gcp.spring.gcp.grpc.ecosystem;

import no.entur.logging.cloud.spring.grpc.ecosystem.RequestResponseGrpcEcosystemAutoConfiguration;
import no.entur.logging.cloud.spring.rr.grpc.AbstractRequestResponseGrpcSinkAutoConfiguration;
import no.entur.logging.cloud.spring.rr.grpc.RequestResponseGrpcAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:request-response.gcp.properties", ignoreResourceNotFound = false)
@AutoConfigureBefore(RequestResponseGrpcAutoConfiguration.class)
public class RequestResponseGcpGrpcEcosystemAutoConfiguration extends AbstractRequestResponseGrpcSinkAutoConfiguration {
}
