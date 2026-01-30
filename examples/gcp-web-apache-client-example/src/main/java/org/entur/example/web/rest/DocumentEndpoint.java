package org.entur.example.web.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/document")
public class DocumentEndpoint {

    private final static Logger logger = LoggerFactory.getLogger(DocumentEndpoint.class);

	@Autowired
	private CloseableHttpClient httpclient;

	private JsonMapper mapper = JsonMapper.builder().build();

	@PostMapping("/some/method")
	public MyEntity someMessage(@RequestBody MyEntity entity, HttpServletRequest request) throws Exception {
		logger.trace("Hello entity with secret / trace");
		logger.debug("Hello entity with secret / debug");
		logger.info("Hello entity with secret / info");
		logger.warn("Hello entity with secret / warn");
		logger.error("Hello entity with secret / error");

		HttpPost httpPost = new HttpPost("http://127.0.0.1:" + request.getServerPort() +  "/api/dummy-service/some/method");

		httpPost.setEntity(new StringEntity(mapper.writeValueAsString(entity)));
		httpPost.setHeader("Accept", "application/json");
		httpPost.setHeader("Content-type", "application/json");

		try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
			HttpEntity responseEntity = response.getEntity();
			String string = EntityUtils.toString(responseEntity);

			logger.info("Downstream service says " + string);

			// Ensure that the stream is fully consumed
			EntityUtils.consume(responseEntity);
		}

		entity.setName("Entur response");
		return entity;
	}



}