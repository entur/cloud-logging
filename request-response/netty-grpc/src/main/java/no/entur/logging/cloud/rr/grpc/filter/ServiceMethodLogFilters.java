package no.entur.logging.cloud.rr.grpc.filter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Log filters and payload filters per service and per method, for the server and client logging filters builders.
 * <br><br>
 * Log filters and payload filters are resolved independently, the most specific first: method, service, then default.
 * Payload filters configured here take precedence over the payload filters of the log filters.
 */

class ServiceMethodLogFilters {

    private static class Values<T> {

        private final Map<String, T> services = new HashMap<>();
        private final Map<String, Map<String, T>> methods = new HashMap<>();
        private T defaultValue;

        void put(String serviceName, Set<String> methodNames, T value) {
            if (methodNames == null || methodNames.isEmpty()) {
                services.put(serviceName, value);
            } else {
                Map<String, T> map = methods.computeIfAbsent(serviceName, k -> new HashMap<>());
                for (String methodName : methodNames) {
                    map.put(methodName, value);
                }
            }
        }

        T get(String serviceName, String methodName) {
            if (methodName != null) {
                Map<String, T> map = methods.get(serviceName);
                if (map != null) {
                    T value = map.get(methodName);
                    if (value != null) {
                        return value;
                    }
                }
            }
            if (serviceName != null) {
                T value = services.get(serviceName);
                if (value != null) {
                    return value;
                }
            }
            return defaultValue;
        }

        void addNames(Map<String, Set<String>> names) {
            for (String serviceName : services.keySet()) {
                names.computeIfAbsent(serviceName, k -> new HashSet<>());
            }
            for (Map.Entry<String, Map<String, T>> entry : methods.entrySet()) {
                names.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue().keySet());
            }
        }
    }

    private final Values<GrpcLogFilter> logFilters = new Values<>();
    private final Values<GrpcPayloadFilter> requestPayloadFilters = new Values<>();
    private final Values<GrpcPayloadFilter> responsePayloadFilters = new Values<>();

    void setDefaultLogFilter(GrpcLogFilter filter) {
        logFilters.defaultValue = filter;
    }

    GrpcLogFilter getDefaultLogFilter() {
        return logFilters.defaultValue;
    }

    /**
     * @param requestFilter  request payload filter, or null for the payload filter of the log filter
     * @param responseFilter response payload filter, or null for the payload filter of the log filter
     */

    void setDefaultPayloadFilters(GrpcPayloadFilter requestFilter, GrpcPayloadFilter responseFilter) {
        requestPayloadFilters.defaultValue = requestFilter;
        responsePayloadFilters.defaultValue = responseFilter;
    }

    void putLogFilter(String serviceName, Set<String> methodNames, GrpcLogFilter filter) {
        logFilters.put(serviceName, methodNames, filter);
    }

    /**
     * @param serviceName    service name
     * @param methodNames    method names, or empty for the whole service
     * @param requestFilter  request payload filter, or null to fall back to the service (or default) request payload filter
     * @param responseFilter response payload filter, or null to fall back to the service (or default) response payload filter
     */

    void putPayloadFilters(String serviceName, Set<String> methodNames, GrpcPayloadFilter requestFilter, GrpcPayloadFilter responseFilter) {
        if (requestFilter != null) {
            requestPayloadFilters.put(serviceName, methodNames, requestFilter);
        }
        if (responseFilter != null) {
            responsePayloadFilters.put(serviceName, methodNames, responseFilter);
        }
    }

    /**
     * @param serviceName service name, or null for the default
     * @param methodName  method name, or null for the service
     * @return the log filter, with payload filters
     */

    GrpcLogFilter resolve(String serviceName, String methodName) {
        GrpcLogFilter logFilter = logFilters.get(serviceName, methodName);

        GrpcPayloadFilter requestPayloadFilter = requestPayloadFilters.get(serviceName, methodName);
        GrpcPayloadFilter responsePayloadFilter = responsePayloadFilters.get(serviceName, methodName);
        if (requestPayloadFilter == null && responsePayloadFilter == null) {
            return logFilter;
        }
        if (requestPayloadFilter == null) {
            requestPayloadFilter = logFilter.getRequestPayloadFilter();
        }
        if (responsePayloadFilter == null) {
            responsePayloadFilter = logFilter.getResponsePayloadFilter();
        }
        return logFilter.withPayloadFilters(requestPayloadFilter, responsePayloadFilter);
    }

    /**
     * @return the configured service names, with their configured method names
     */

    Map<String, Set<String>> getNames() {
        Map<String, Set<String>> names = new HashMap<>();
        logFilters.addNames(names);
        requestPayloadFilters.addNames(names);
        responsePayloadFilters.addNames(names);
        return names;
    }
}
