package no.entur.logging.cloud.gcp.spring.gcp.grpc.spring;

import no.entur.logging.cloud.spring.rr.grpc.GrpcLoggingCloudProperties;
import no.entur.logging.cloud.spring.rr.grpc.RequestResponseGrpcAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureBefore(RequestResponseGrpcAutoConfiguration.class)
public class RequestResponseGcpGrpcSpringAutoConfiguration {

    @Bean
    public GrpcLoggingCloudProperties grpcLoggingCloudProperties() {
        GrpcLoggingCloudProperties c = new GrpcLoggingCloudProperties();
        // hard limit as of April 2026: 256 KiB - where KiB is 1024 bytes
        // Body: set it to 224 KiB to be on the safe side,
        // and to allow for a lot of overhead in the logging framework, MDC, HTTP headers, bytes vs chars, etc.
        c.setMaxBodySize(229376);
        c.setMaxSize(262144 - 2 * 1024); // inaccurate + currently not enforced
        return c;
    }
}
