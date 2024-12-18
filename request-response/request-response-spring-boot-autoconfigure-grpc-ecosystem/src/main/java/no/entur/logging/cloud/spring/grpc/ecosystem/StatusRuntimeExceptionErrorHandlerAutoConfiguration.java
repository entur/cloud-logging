package no.entur.logging.cloud.spring.grpc.ecosystem;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import net.devh.boot.grpc.server.autoconfigure.GrpcAdviceAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Configuration;

//@ConditionalOnMissingErrorHandler(StatusRuntimeException.class)
@Configuration
@AutoConfigureBefore(GrpcAdviceAutoConfiguration.class)
public class StatusRuntimeExceptionErrorHandlerAutoConfiguration {

    @GrpcAdvice
    public static class StatusRuntimeExceptionGrpcServiceAdvice {
        @java.lang.SuppressWarnings("all")

        @GrpcExceptionHandler
        public Status handle(StatusRuntimeException e) {
            return e.getStatus();
        }
    }
}