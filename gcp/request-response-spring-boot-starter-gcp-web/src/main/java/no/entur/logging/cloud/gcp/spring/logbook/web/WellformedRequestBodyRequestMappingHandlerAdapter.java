package no.entur.logging.cloud.gcp.spring.logbook.web;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

public class WellformedRequestBodyRequestMappingHandlerAdapter extends RequestMappingHandlerAdapter {

    @Override
    protected ServletInvocableHandlerMethod createInvocableHandlerMethod(HandlerMethod handlerMethod) {
        return new WellformedRequestBodyServletInvocableHandlerMethod(handlerMethod);
    }
}
