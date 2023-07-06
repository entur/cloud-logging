package org.entur.logbook.example.rest;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
		logger.info("Hello entity with secret");
		
		entity.setName("Entur respons");
		return entity;
	}

}