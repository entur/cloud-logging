package org.entur.example.web.rest;

import org.jspecify.annotations.NonNull;

public class MyEntity {

	@NonNull
	private String secret;

	@NonNull
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
