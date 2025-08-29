package no.entur.logging.cloud.spring.logbook.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "entur.logging.request-response.format")
public class FormatProperties {
	private ServerFormatProperties server = new ServerFormatProperties();
	private ClientFormatProperties client = new ClientFormatProperties();

	public ServerFormatProperties getServer() {
		return server;
	}

	public void setServer(ServerFormatProperties server) {
		this.server = server;
	}

	public ClientFormatProperties getClient() {
		return client;
	}

	public void setClient(ClientFormatProperties client) {
		this.client = client;
	}
}
