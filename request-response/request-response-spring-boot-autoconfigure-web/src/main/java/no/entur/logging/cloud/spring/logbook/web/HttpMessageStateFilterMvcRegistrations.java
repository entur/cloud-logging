package no.entur.logging.cloud.spring.logbook.web;
import org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

public class HttpMessageStateFilterMvcRegistrations implements WebMvcRegistrations {

	@Override
	public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
		return new HttpMessageStateFilterRequestMappingHandlerAdapter();
	}
}