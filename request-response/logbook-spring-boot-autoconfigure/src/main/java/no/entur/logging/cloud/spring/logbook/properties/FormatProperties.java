package no.entur.logging.cloud.spring.logbook.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "entur.logging.request-response.format")
public class FormatProperties {
	private MessageProperties server = new MessageProperties();
	private MessageProperties client = new MessageProperties();

	public MessageProperties getServer() {
		return server;
	}

	public void setServer(MessageProperties server) {
		this.server = server;
	}

	public MessageProperties getClient() {
		return client;
	}

	public void setClient(MessageProperties client) {
		this.client = client;
	}
}
