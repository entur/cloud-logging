package no.entur.logging.cloud.gcp.spring.logbook.web;

import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

public class HttpMessageStateFilterMvcRegistrations implements WebMvcRegistrations {

	@Override
	public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
		return new HttpMessageStateFilterRequestMappingHandlerAdapter();
	}
}