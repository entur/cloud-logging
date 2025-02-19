package no.entur.logging.cloud.gcp.spring.gcp.grpc.lognet;

import no.entur.logging.cloud.spring.grpc.lognet.RequestResponseGrpcLognetAutoConfiguration;
import no.entur.logging.cloud.spring.rr.grpc.AbstractRequestResponseGrpcSinkAutoConfiguration;
import no.entur.logging.cloud.spring.rr.grpc.GrpcLoggingCloudProperties;
import no.entur.logging.cloud.spring.rr.grpc.RequestResponseGrpcAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@AutoConfigureBefore(RequestResponseGrpcAutoConfiguration.class)
public class RequestResponseGcpGrpcLognetAutoConfiguration {

    @Bean
    public GrpcLoggingCloudProperties grpcLoggingCloudProperties() {
        GrpcLoggingCloudProperties c = new GrpcLoggingCloudProperties();
        // subtract a few kb for headers and other wrapping
        c.setMaxBodySize(131072 - 2 * 1024);
        c.setMaxSize(131072);
        return c;
    }
}
