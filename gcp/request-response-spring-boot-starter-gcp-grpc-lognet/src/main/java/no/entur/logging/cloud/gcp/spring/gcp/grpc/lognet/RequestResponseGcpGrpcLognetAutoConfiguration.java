package no.entur.logging.cloud.gcp.spring.gcp.grpc.lognet;

import no.entur.logging.cloud.spring.grpc.lognet.AbstractRequestResponseGrpcLognetSinkAutoConfiguration;
import no.entur.logging.cloud.spring.grpc.lognet.RequestResponseGrpcLognetAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:request-response.gcp.properties", ignoreResourceNotFound = false)
@AutoConfigureBefore(RequestResponseGrpcLognetAutoConfiguration.class)
public class RequestResponseGcpGrpcLognetAutoConfiguration extends AbstractRequestResponseGrpcLognetSinkAutoConfiguration {
}
