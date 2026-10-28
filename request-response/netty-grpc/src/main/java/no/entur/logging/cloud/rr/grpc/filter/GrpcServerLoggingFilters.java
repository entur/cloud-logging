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

		private final ServiceMethodLogFilters filters = new ServiceMethodLogFilters();

		public Builder defaultFilter(GrpcLogFilter filter) {
			filters.setDefaultLogFilter(filter);
			return this;
		}

		/**
		 * Filter request and response payloads of all services, unless another payload filter is configured for the service or method.
		 *
		 * @param filter payload filter
		 * @return this builder
		 */

		public Builder defaultPayloadFilter(GrpcPayloadFilter filter) {
			return defaultPayloadFilter(filter, filter);
		}

		public Builder defaultPayloadFilter(GrpcPayloadFilter requestFilter, GrpcPayloadFilter responseFilter) {
			filters.setDefaultPayloadFilters(requestFilter, responseFilter);
			return this;
		}

		/**
		 * Filter request and response payloads of a service, independently of the logging configured for the service.
		 *
		 * @param serviceName service name
		 * @param filter payload filter
		 * @return this builder
		 */

		public Builder payloadFilterForService(String serviceName, GrpcPayloadFilter filter) {
			return payloadFilterForService(serviceName, filter, filter, Collections.emptySet());
		}

		public Builder payloadFilterForService(String serviceName, GrpcPayloadFilter filter, String ... methods) {
			return payloadFilterForService(serviceName, filter, filter, toSet(methods));
		}

		public Builder payloadFilterForService(String serviceName, GrpcPayloadFilter filter, MethodDescriptor ... methods) {
			return payloadFilterForService(serviceName, filter, filter, toSet(methods));
		}

		/**
		 * Filter request and/or response payloads of a service or some of its methods, independently of the logging configured
		 * for the service or methods. The most specific payload filter applies: method, service, then default.
		 *
		 * @param serviceName service name
		 * @param requestFilter request payload filter, or null to fall back to the service (or default) request payload filter
		 * @param responseFilter response payload filter, or null to fall back to the service (or default) response payload filter
		 * @param methods method names, or empty for the whole service
		 * @return this builder
		 */

		public Builder payloadFilterForService(String serviceName, GrpcPayloadFilter requestFilter, GrpcPayloadFilter responseFilter, Set<String> methods) {
			filters.putPayloadFilters(serviceName, methods, requestFilter, responseFilter);
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
			filters.putLogFilter(serviceName, methods, filter);
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
			if(filters.getDefaultLogFilter() == null) {
				throw new IllegalStateException("Expected default behaviour");
			}

			Map<String, ServiceFilter> results = new HashMap<>();

			for (Map.Entry<String, Set<String>> entry : filters.getNames().entrySet()) {
				String serviceName = entry.getKey();

				Map<String, GrpcLogFilter> methodFilters = new HashMap<>();
				for (String methodName : entry.getValue()) {
					methodFilters.put(methodName, filters.resolve(serviceName, methodName));
				}

				ServiceFilter serviceFilter = new ServiceFilter();
				serviceFilter.setServiceFilter(filters.resolve(serviceName, null));
				serviceFilter.setMethodFilters(methodFilters);
				results.put(serviceName, serviceFilter);
			}

			return new GrpcServerLoggingFilters(results, filters.resolve(null, null));
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
