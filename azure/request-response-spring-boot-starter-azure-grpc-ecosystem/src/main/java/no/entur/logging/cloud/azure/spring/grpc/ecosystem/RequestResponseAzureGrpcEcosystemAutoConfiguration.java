package no.entur.logging.cloud.azure.spring.grpc.ecosystem;

import no.entur.logging.cloud.spring.grpc.ecosystem.RequestResponseGrpcEcosystemAutoConfiguration;
import no.entur.logging.cloud.spring.rr.grpc.AbstractRequestResponseGrpcSinkAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:request-response.azure.properties", ignoreResourceNotFound = false)
@AutoConfigureBefore(RequestResponseGrpcEcosystemAutoConfiguration.class)
public class RequestResponseAzureGrpcEcosystemAutoConfiguration extends AbstractRequestResponseGrpcSinkAutoConfiguration {
}