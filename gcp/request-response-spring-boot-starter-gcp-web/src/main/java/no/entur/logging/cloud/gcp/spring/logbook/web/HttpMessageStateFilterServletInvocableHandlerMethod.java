package no.entur.logging.cloud.gcp.spring.logbook.web;

import no.entur.logging.cloud.logbook.ondemand.state.DefaultHttpMessageStateSupplier;
import no.entur.logging.cloud.logbook.ondemand.state.HttpMessageState;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

import java.util.Arrays;

/**
 *
 * Listen in on the databinding, databinding.
 *
 */
public class HttpMessageStateFilterServletInvocableHandlerMethod extends ServletInvocableHandlerMethod {

    public HttpMessageStateFilterServletInvocableHandlerMethod(HandlerMethod handlerMethod) {
        super(handlerMethod);
    }

    @Nullable
    public Object invokeForRequest(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {
        // TODO detect request body annotation?

        DefaultHttpMessageStateSupplier httpMessageStateSupplier = (DefaultHttpMessageStateSupplier) request.getAttribute(HttpMessageStateFilter.HTTP_MESSAGE_STATE, RequestAttributes.SCOPE_REQUEST);
        if(httpMessageStateSupplier == null) {
            return super.invokeForRequest(request, mavContainer, providedArgs);
        }

        try {
            Object[] args = this.getMethodArgumentValues(request, mavContainer, providedArgs);
            if (logger.isTraceEnabled()) {
                logger.trace("Arguments: " + Arrays.toString(args));
            }

            Object o = this.doInvoke(args);

            httpMessageStateSupplier.setBodySyntaxState(HttpMessageState.VALID);

            return o;
        } catch(Exception e) {
            // something failed, might have been the databinding. But might be well-formed JSON still.
            // TODO can we detech whether this was a parse or databinding error?

            httpMessageStateSupplier.setBodySyntaxState(HttpMessageState.UNKNOWN);

            throw e;
        }
    }

}
