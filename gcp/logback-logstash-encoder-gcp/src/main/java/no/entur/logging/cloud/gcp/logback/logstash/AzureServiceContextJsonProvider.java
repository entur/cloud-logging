package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.FieldNamesAware;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.fieldnames.LogstashFieldNames;

import java.io.IOException;

/**
 * Add service-context. This improves error reporting views in GCP.
 * 
 * @see <a href="https://cloud.google.com/error-reporting/reference/rest/v1beta1/ServiceContext">https://cloud.google.com/error-reporting/reference/rest/v1beta1/ServiceContext</a>
 * @see <a href="https://kubernetes.io/docs/concepts/containers/container-environment-variables/">https://kubernetes.io/docs/concepts/containers/container-environment-variables/</a>
 */

public class AzureServiceContextJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> implements FieldNamesAware<LogstashFieldNames> {

	public static final String SERVICE_CONTEXT = "serviceContext";

	private static final String UNDEFINED = "undefined";
	
	private String service = null;
	private String version = null;

	// versionType: Value is set automatically for incoming errors and must not be set when reporting errors.
	
	public AzureServiceContextJsonProvider() {
		setFieldName(SERVICE_CONTEXT);
	}

	public void setService(String service) {
		if(service == null || service.equals(UNDEFINED)) {
			autodetectService();
		} else {
			this.service = service;
		}
	}

	public void autodetectService() {
		this.service = getHostNameFromEnvironment();
	}

	public String getHostNameFromEnvironment() {
		try {
			// prefer getting system variable rather than the actual network address
			String hostname = System.getProperty("HOSTNAME");
			if(hostname == null) {
				hostname = System.getenv("HOSTNAME");
			}
			
			return parseServiceNameFromHostname(hostname); 
		} catch(Exception e) {
			// ignore
		}
		return null;
	}

	public static String parseServiceNameFromHostname(String host) {
		if(host == null || host.isEmpty()) {
			return null;
		}
		
		// assume host-aadf-c
		
		int first = host.lastIndexOf('-');
		if(first > 0) {
			int second = host.lastIndexOf('-', first - 1);
			if(second > 0) {
				return host.substring(0, second);
			}
		}
		return null;
	}
	
	@Override
	public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
		if(service != null) {
			generator.writeObjectFieldStart(getFieldName());
			
			JsonWritingUtils.writeStringField(generator, "service", service);
			
			if(version != null) {
				JsonWritingUtils.writeStringField(generator, "version", version);
			}
			generator.writeEndObject();
		}
	}
	
	@Override
	public void setFieldNames(LogstashFieldNames fieldNames) {
	}
		
	public void setVersion(String version) {
		if(version == null || version.equals(UNDEFINED)) {
			this.version = null;
		} else {
			this.version = version;
		}

	}
	
}