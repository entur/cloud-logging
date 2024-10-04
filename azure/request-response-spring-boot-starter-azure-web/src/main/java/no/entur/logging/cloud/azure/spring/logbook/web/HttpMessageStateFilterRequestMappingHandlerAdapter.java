package no.entur.logging.cloud.azure.spring.logbook.web;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

public class HttpMessageStateFilterRequestMappingHandlerAdapter extends RequestMappingHandlerAdapter {

    @Override
    protected ServletInvocableHandlerMethod createInvocableHandlerMethod(HandlerMethod handlerMethod) {
        return new HttpMessageStateFilterServletInvocableHandlerMethod(handlerMethod);
    }
}
