package no.entur.logging.cloud.azure.spring.grpc.lognet;

import no.entur.logging.cloud.spring.grpc.lognet.AbstractRequestResponseGrpcLognetSinkAutoConfiguration;
import no.entur.logging.cloud.spring.grpc.lognet.RequestResponseGrpcLognetAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:request-response.azure.properties", ignoreResourceNotFound = false)
@AutoConfigureBefore(RequestResponseGrpcLognetAutoConfiguration.class)
public class RequestResponseAzureGrpcLognetAutoConfiguration extends AbstractRequestResponseGrpcLognetSinkAutoConfiguration {


}
