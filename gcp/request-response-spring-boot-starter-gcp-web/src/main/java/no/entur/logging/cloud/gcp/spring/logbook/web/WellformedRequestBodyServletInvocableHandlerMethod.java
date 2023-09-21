package no.entur.logging.cloud.gcp.spring.logbook.web;

import no.entur.logging.cloud.gcp.spring.web.OndemandFilter;
import org.springframework.context.MessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * Listen in on the databinding, databinding.
 *
 */
public class WellformedRequestBodyServletInvocableHandlerMethod extends ServletInvocableHandlerMethod {

    public WellformedRequestBodyServletInvocableHandlerMethod(HandlerMethod handlerMethod) {
        super(handlerMethod);
    }

    @Nullable
    public Object invokeForRequest(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {
        // TODO detect request body annotation?

        AtomicBoolean wellformed = (AtomicBoolean) request.getAttribute(OndemandFilter.WELLFORMED_INDICATOR, RequestAttributes.SCOPE_REQUEST);
        if(wellformed == null) {
            return super.invokeForRequest(request, mavContainer, providedArgs);
        }

        try {
            Object[] args = this.getMethodArgumentValues(request, mavContainer, providedArgs);
            if (logger.isTraceEnabled()) {
                logger.trace("Arguments: " + Arrays.toString(args));
            }

            Object o = this.doInvoke(args);

            wellformed.set(true);

            return o;
        } catch(Exception e) {
            // something failed, might have been the databinding. But might be well-formed JSON still.
            // TODO can we detech whether this was a parse or databinding error?

            wellformed.set(false);

            throw e;
        }
    }

}
