package org.entur.example.web.rest;

import org.codehaus.commons.nullanalysis.NotNull;

public class MyEntity {

	@NotNull
	private String secret;

	@NotNull
	private String name;
	
	public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}
	public String getName() {
		return name;
	}
	public void setName(String key) {
		this.name = key;
	}


	
}
