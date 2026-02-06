package no.entur.logging.cloud.spring.grpc.spring;

import no.entur.logging.cloud.spring.rr.grpc.RequestResponseGrpcExceptionHandlerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.grpc.server.autoconfigure.exception.GrpcExceptionHandlerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;

/**
 *
 * We need the proper error handling to run before the response is logged, i.e. to make sure no exception is thrown through the
 * request-response interceptor, we wrap the system error handler to run in an additional interceptor and make it run first.
 * .
 *
 */

@Configuration
@AutoConfigureAfter(GrpcExceptionHandlerAutoConfiguration.class)
public class RequestResponseExceptionHandlerGrpcSpringAutoConfiguration {

    @Value("${entur.logging.request-response.grpc.server.exception-handler.interceptor-order:400}")
    private int exceptionInterceptorOrder;

    @Bean
    @ConditionalOnProperty(name = {"entur.logging.request-response.grpc.server.exception-handler.enabled"}, havingValue = "true", matchIfMissing = true)
    public RequestResponseGrpcExceptionHandlerInterceptor requestResponseGrpcExceptionHandlerInterceptor(GrpcExceptionHandlerInterceptor interceptor) {
        return new RequestResponseGrpcExceptionHandlerInterceptor(interceptor, exceptionInterceptorOrder);
    }

}