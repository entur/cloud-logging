package no.entur.logging.cloud.azure.spring.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import no.entur.logging.cloud.azure.spring.grpc.annotate.ConditionalOnMissingErrorHandlerForExactException;
import org.lognet.springboot.grpc.autoconfigure.GRpcAutoConfiguration;
import org.lognet.springboot.grpc.recovery.GRpcExceptionHandler;
import org.lognet.springboot.grpc.recovery.GRpcExceptionScope;
import org.lognet.springboot.grpc.recovery.GRpcServiceAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureBefore(GRpcAutoConfiguration.class)
public class GrpcLoggingAutoConfiguration {

    // catch all and log stacktrace
    // note: this will eclipse all other users of the annotation ConditionalOnMissingErrorHandler
    @ConditionalOnMissingErrorHandlerForExactException(Exception.class)
    @Configuration
    static class DefaultAccessDeniedErrorHandlerConfig {

        @GRpcServiceAdvice
        private static class ExceptionHandler {

            private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);
            @GRpcExceptionHandler
            public Status handle (Exception e, GRpcExceptionScope scope){
                // assume seeing Exception is unexpected
                LOGGER.error("Call to " + scope.getMethodDescriptor().getFullMethodName() + " resulted in " + e.getClass().getSimpleName() + ", returning status " + Status.INTERNAL.getCode().name(), e);

                return Status.INTERNAL;
            }

        }
    }

    @ConditionalOnMissingErrorHandlerForExactException(StatusRuntimeException.class)
    @Configuration
    static class DefaultStatusErrorHandlerConfiguration {
        @GRpcServiceAdvice
        public static class StatusRuntimeExceptionHandler {

            private static final Logger LOGGER = LoggerFactory.getLogger(StatusRuntimeExceptionHandler.class);
            @GRpcExceptionHandler
            public Status handle(StatusRuntimeException e, GRpcExceptionScope scope) {
                // assume seeing StatusRuntimeException is unexpected

                Status status = e.getStatus();
                String description = status.getDescription();

                String message = "Call to " + scope.getMethodDescriptor().getFullMethodName() + " resulted in " + e.getClass().getSimpleName() + ": " + status.getCode().name() + (description != null ? (" " + status.getDescription()) : "");
                if(status.getCode() == Status.Code.INTERNAL) {
                    LOGGER.error(message, e);
                } else {
                    LOGGER.info(message, e);
                }

                return status.withDescription(e.getMessage());
            }
        }
    }



}
