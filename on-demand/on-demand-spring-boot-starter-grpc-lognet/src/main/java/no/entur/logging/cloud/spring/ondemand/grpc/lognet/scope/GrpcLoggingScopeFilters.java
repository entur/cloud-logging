package no.entur.logging.cloud.spring.ondemand.grpc.lognet.scope;

import java.util.HashMap;
import java.util.Map;

public class GrpcLoggingScopeFilters {

    private Map<String, GrpcLoggingScopeFilter> services = new HashMap<>();

    private Map<String, Map<String, GrpcLoggingScopeFilter>> methods = new HashMap<>();

    private GrpcLoggingScopeFilter defaultFilter;

    public void addFilter(String serviceName, GrpcLoggingScopeFilter filter) {
        services.put(serviceName, filter);
    }

    public void addFilter(String serviceName, String methodName, GrpcLoggingScopeFilter filter) {
        Map<String, GrpcLoggingScopeFilter> map = methods.get(serviceName);
        if(map == null) {
            map = new HashMap<>();
            methods.put(serviceName, map);
        }
        map.put(methodName, filter);
    }

    public void setDefaultFilter(GrpcLoggingScopeFilter defaultFilter) {
        this.defaultFilter = defaultFilter;
    }

    public GrpcLoggingScopeFilter getFilter(String serviceName, String methodName) {

        GrpcLoggingScopeFilter grpcLoggingScopeFilter = services.get(serviceName);
        if(grpcLoggingScopeFilter != null) {
            return grpcLoggingScopeFilter;
        }

        Map<String, GrpcLoggingScopeFilter> serviceMethods = methods.get(serviceName);
        if(serviceMethods != null) {
            grpcLoggingScopeFilter = serviceMethods.get(methodName);
            if(grpcLoggingScopeFilter != null) {
                return grpcLoggingScopeFilter;
            }
        }

        return defaultFilter;
    }

}
