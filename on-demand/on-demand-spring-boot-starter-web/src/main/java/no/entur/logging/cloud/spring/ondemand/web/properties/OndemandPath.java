package no.entur.logging.cloud.spring.ondemand.web.properties;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class OndemandPath {

	private boolean enabled = true;
	private String matcher;

	@NestedConfigurationProperty
	private OndemandSuccess success = new OndemandSuccess();

	@NestedConfigurationProperty
	private OndemandFailure failure = new OndemandFailure();

	@NestedConfigurationProperty
	private OndemandTroubleshoot troubleshoot = new OndemandTroubleshoot();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getMatcher() {
		return matcher;
	}

	public void setMatcher(String matcher) {
		this.matcher = matcher;
	}

	public OndemandSuccess getSuccess() {
		return success;
	}

	public void setSuccess(OndemandSuccess success) {
		this.success = success;
	}

	public OndemandFailure getFailure() {
		return failure;
	}

	public void setFailure(OndemandFailure failure) {
		this.failure = failure;
	}

	public void setTroubleshoot(OndemandTroubleshoot troubleshoot) {
		this.troubleshoot = troubleshoot;
	}

	public OndemandTroubleshoot getTroubleshoot() {
		return troubleshoot;
	}
}
