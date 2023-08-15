package no.entur.logging.cloud.gcp.spring.grpc.lognet.properties;

import java.util.ArrayList;
import java.util.List;

public class OndemandPath {

	private boolean enabled = true;
	private List<ServiceMatcherConfiguration> services = new ArrayList<>();

	private OndemandSuccess success = new OndemandSuccess();
	private OndemandFailure failure = new OndemandFailure();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<ServiceMatcherConfiguration> getServices() {
		return services;
	}

	public void setServices(List<ServiceMatcherConfiguration> services) {
		this.services = services;
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
}
