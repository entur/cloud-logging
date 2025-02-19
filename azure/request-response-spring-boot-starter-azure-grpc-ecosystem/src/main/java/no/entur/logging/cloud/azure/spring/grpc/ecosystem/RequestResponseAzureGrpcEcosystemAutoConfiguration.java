package no.entur.logging.cloud.azure.spring.grpc.ecosystem;

import no.entur.logging.cloud.spring.grpc.ecosystem.RequestResponseGrpcEcosystemAutoConfiguration;
import no.entur.logging.cloud.spring.rr.grpc.AbstractRequestResponseGrpcSinkAutoConfiguration;
import no.entur.logging.cloud.spring.rr.grpc.GrpcLoggingCloudProperties;
import no.entur.logging.cloud.spring.rr.grpc.RequestResponseGrpcAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@AutoConfigureBefore(RequestResponseGrpcAutoConfiguration.class)
public class RequestResponseAzureGrpcEcosystemAutoConfiguration {

    @Bean
    public GrpcLoggingCloudProperties grpcLoggingCloudProperties() {
        GrpcLoggingCloudProperties c = new GrpcLoggingCloudProperties();
        // subtract a few kb for headers and other wrapping
        c.setMaxBodySize(16384 - 2 * 1024);
        c.setMaxSize(16384);
        return c;
    }

}