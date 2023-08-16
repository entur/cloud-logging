package no.entur.logging.cloud.gcp.spring.grpc.lognet.properties;

import java.util.ArrayList;
import java.util.List;

public class OndemandPath {

	private boolean enabled = true;

	private String serviceName;

	private List<String> methodNames = new ArrayList<>();

	private OndemandSuccess success = new OndemandSuccess();
	private OndemandFailure failure = new OndemandFailure();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
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

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public List<String> getMethodNames() {
		return methodNames;
	}

	public void setMethodNames(List<String> methodNames) {
		this.methodNames = methodNames;
	}
}
