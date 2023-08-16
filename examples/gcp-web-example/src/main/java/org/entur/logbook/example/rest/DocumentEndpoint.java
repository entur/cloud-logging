package org.entur.logbook.example.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Path("/document")
public class DocumentEndpoint {

    private final static Logger logger = LoggerFactory.getLogger(DocumentEndpoint.class);

	@POST
    @Path("/some/method")
	@Produces(MediaType.APPLICATION_JSON)
	public MyEntity someMessage(MyEntity entity) {
		logger.trace("Hello entity with secret / trace");
		logger.debug("Hello entity with secret / debug");
		logger.info("Hello entity with secret / info");
		logger.warn("Hello entity with secret / warn");

		entity.setName("Entur response");
		return entity;
	}

	@GET
	@Path("/some/error")
	@Produces(MediaType.APPLICATION_JSON)
	public Response errorMethod() throws InterruptedException {
		System.out.flush();
		System.out.println("System out before endpoint logging");

		logger.trace("This message should be ignored / trace");
		logger.debug("This message should be ignored / debug");
		logger.info("This message should be delayed / info");
		logger.warn("This message should be logged / warn");
		logger.error("This message should be logged / error");

		Thread.sleep(1000);
		System.out.println("System out after endpoint logging + 1000ms");

		return Response.serverError().build(); // translates to 404 because no /error resource
	}



}