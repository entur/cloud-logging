package no.entur.logging.cloud.rr.grpc.filter;

import io.grpc.MethodDescriptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GrpcServerLoggingFilters {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static GrpcServerLoggingFilters full() {
		return new GrpcServerLoggingFilters.Builder().fullDefaultLogging().build();
	}

	public static GrpcServerLoggingFilters classic() {
		return new GrpcServerLoggingFilters.Builder().classicDefaultLogging().build();
	}

	public static class Builder {

		private Map<String, GrpcLogFilter> services = new HashMap<>(); // i.e. per-service defaults

		private Map<String, Map<String, GrpcLogFilter>> methods = new HashMap<>();

		private GrpcLogFilter defaultFilter; // i.e. global service defaults

		public Builder defaultFilter(GrpcLogFilter filter) {
			this.defaultFilter = filter;
			return this;
		}


		public Builder requestSummaryLoggingForService(String serviceName) {
			return loggingForService(serviceName, GrpcLogFilter.REQUEST_SUMMARY, Collections.emptySet());
		}

		public Builder requestSummaryLoggingForService(String serviceName, String ... methods) {
			return loggingForService(serviceName, GrpcLogFilter.REQUEST_SUMMARY, methods);
		}

		public Builder fullLoggingForService(String serviceName) {
			return loggingForService(serviceName, GrpcLogFilter.FULL, Collections.emptySet());
		}

		public Builder fullLoggingForService(String serviceName, String ... methods) {
			return loggingForService(serviceName, GrpcLogFilter.FULL, methods);
		}

		public Builder classicLoggingForService(String serviceName, String ... methods) {
			return loggingForService(serviceName, GrpcLogFilter.REQUEST_RESPONSE, methods);
		}

		public Builder classicLoggingForService(String serviceName) {
			return loggingForService(serviceName, GrpcLogFilter.REQUEST_RESPONSE, Collections.emptySet());
		}

		public Builder summaryLoggingForService(String serviceName, String ... methods) {
			return loggingForService(serviceName, GrpcLogFilter.SUMMARY, methods);
		}

		public Builder summaryLoggingForService(String serviceName) {
			return loggingForService(serviceName, GrpcLogFilter.SUMMARY, Collections.emptySet());
		}

		public Builder noLoggingForService(String serviceName, String ... methods) {
			return loggingForService(serviceName, GrpcLogFilter.NONE, methods);
		}


		public Builder noLoggingForService(String serviceName) {
			return loggingForService(serviceName, GrpcLogFilter.NONE, Collections.emptySet());
		}

		public Builder requestSummaryLoggingForService(String serviceName, MethodDescriptor ... methods) {
			return loggingForService(serviceName, GrpcLogFilter.REQUEST_SUMMARY, methods);
		}

		public Builder fullLoggingForService(String serviceName, MethodDescriptor ... methods) {
			return loggingForService(serviceName, GrpcLogFilter.FULL, methods);
		}

		public Builder classicLoggingForService(String serviceName, MethodDescriptor ... methods) {
			return loggingForService(serviceName, GrpcLogFilter.REQUEST_RESPONSE, methods);
		}

		public Builder summaryLoggingForService(String serviceName, MethodDescriptor... methods) {
			return loggingForService(serviceName, GrpcLogFilter.SUMMARY, methods);
		}

		public Builder noLoggingForService(String serviceName, MethodDescriptor ... methods) {
			return loggingForService(serviceName, GrpcLogFilter.NONE, methods);
		}

		public Builder loggingForService(String serviceName, GrpcLogFilter filter, Set<String> methods) {
			if(methods == null || methods.isEmpty()) {
				services.put(serviceName, filter);
			} else {
				Map<String, GrpcLogFilter> map = this.methods.get(serviceName);
				if(map == null) {
					map = new HashMap<>();
					this.methods.put(serviceName, map);
				}
				for (String methodName : methods) {
					map.put(methodName, filter);
				}
			}

			return this;
		}

		public Builder loggingForService(String serviceName, GrpcLogFilter filter, MethodDescriptor ... methods) {
			return loggingForService(serviceName, filter, toSet(methods));
		}

		public Builder loggingForService(String serviceName, GrpcLogFilter filter) {
			return loggingForService(serviceName, filter, Collections.emptySet());
		}

		public Builder loggingForService(String serviceName, GrpcLogFilter filter, String ... methods) {
			return loggingForService(serviceName, filter, toSet(methods));
		}

		public Builder noDefaultLogging() {
			return defaultFilter(GrpcLogFilter.NONE);
		}

		public Builder summaryDefaultLogging() {
			return defaultFilter(GrpcLogFilter.SUMMARY);
		}

		public Builder classicDefaultLogging() {
			return defaultFilter(GrpcLogFilter.REQUEST_RESPONSE);
		}

		public Builder fullDefaultLogging() {
			return defaultFilter(GrpcLogFilter.FULL);
		}

		public Builder requestSummaryDefaultLogging() {
			return defaultFilter(GrpcLogFilter.REQUEST_SUMMARY);
		}

		private static Set<String> toSet(String[] methods) {
			Set<String> objects = new HashSet<>(methods.length * 2);
			for (String method : methods) {
				objects.add(method);
			}
			return objects;
		}

		private static Set<String> toSet(MethodDescriptor[] methods) {
			Set<String> objects = new HashSet<>(methods.length * 2);
			for (MethodDescriptor method : methods) {
				objects.add(method.getBareMethodName());
			}
			return objects;
		}

		public GrpcServerLoggingFilters build() {
			if(defaultFilter == null) {
				throw new IllegalStateException("Expected default behaviour");
			}

			Map<String, ServiceFilter> results = new HashMap<>();

			for (Map.Entry<String, GrpcLogFilter> entry : services.entrySet()) {
				ServiceFilter serviceFilter = results.computeIfAbsent(entry.getKey(), (k) -> new ServiceFilter());
				serviceFilter.setServiceFilter(entry.getValue());
			}

			for (Map.Entry<String, Map<String, GrpcLogFilter>> entry : methods.entrySet()) {
				ServiceFilter serviceFilter = results.computeIfAbsent(entry.getKey(), (k) -> new ServiceFilter());
				serviceFilter.setMethodFilters(entry.getValue());
			}

			return new GrpcServerLoggingFilters(results, defaultFilter);
		}
	}

	private static class ServiceFilter {

		private GrpcLogFilter serviceFilter;

		private Map<String, GrpcLogFilter> methodFilters = Collections.emptyMap();

		public void setServiceFilter(GrpcLogFilter serviceFilter) {
			this.serviceFilter = serviceFilter;
		}

		public void setMethodFilters(Map<String, GrpcLogFilter> methodFilters) {
			this.methodFilters = methodFilters;
		}

		public GrpcLogFilter getFilter(String method) {
			GrpcLogFilter methodFilter = methodFilters.get(method);
			if(methodFilter != null) {
				return methodFilter;
			}
			return serviceFilter;
		}
	}


	protected final Map<String, ServiceFilter> serviceFilters;
	protected final GrpcLogFilter defaultFilter;

	protected GrpcServerLoggingFilters(Map<String, ServiceFilter> serviceFilters, GrpcLogFilter defaultFilter) {
		this.serviceFilters = serviceFilters;
		this.defaultFilter = defaultFilter;
	}


	public GrpcLogFilter getFilter(String serviceName, String serviceMethod) {

		ServiceFilter serviceFilter = serviceFilters.get(serviceName);
		if(serviceFilter == null) {
			return defaultFilter;
		}

		GrpcLogFilter filter = serviceFilter.getFilter(serviceMethod);
		if(filter != null) {
			return filter;
		}

		return defaultFilter;
	}

}
