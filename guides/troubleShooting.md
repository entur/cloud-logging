# Troubleshooting

See also the troubleshooting sections in the getting started guides:
 * [Web troubleshooting](web.md#troubleshooting)
 * [gRPC troubleshooting](gRPC.md#troubleshooting)

## Common issues

### Existing log configuration conflicts
Remove any pre-existing log configuration file (e.g. `logback.xml`, `logback-spring.xml`) before adding the cloud-logging starters. The starters provide their own Logback configuration via `logback-spring.xml` on the classpath, and having two configurations active at the same time will cause unexpected behaviour.

### Missing logs after adding the library
Ensure that the Spring log level properties are set correctly:

```
logging.level.root=INFO
logging.level.my.package=WARN
```

If using on-demand logging, remember that it is disabled by default and must be enabled explicitly (see the getting started guides).

## Fluentbit errors

Fluentbit is the service which translates console logging into Stackdriver (AKA cloud logging) log entries.

### Duplicate `jsonPayload` field names
`Fluentbit` blows up if `jsonPayload` fields are duplicated. Symptoms:

 * truncated text values
 * **missing logs for 2-4 seconds**
   * Unrelated log statements are also lost. 

`Fluentbit` will log warnings/errors, but not in the app which is causing the problems. 

Warnings/Errors like

> Failed to get record: decoder: failed to decode payload: msgpack decode error [pos 757]: runtime error: hash of unhashable type []interface {}

> Failed to get record: decoder: failed to decode payload: msgpack decode error [pos 498]: cannot decode signed integer: unrecognized descriptor byte: b0/string|bytes

indicate this is happening.

<details>
  <summary>Test code to reproduce errors.</summary>

```java
	int counter = 0;
	int counter2 = 0;
	int counter3 = 0;
	int counter4 = 0;
	int counter5 = 0;
	int counter6 = 0;

	@Scheduled(cron = "* * * * * *")
	public void scheduledEverySecond() {
		log.info("Log second " + counter++);
	}

	@Scheduled(cron = "10 * * * * *")
	public void scheduledEvery10Second() {
		
		/**
		 * Duplicate value: same length as "string" from "log message" is "log me"
		 * 
		 * Does not result in lost items. No fluentbit message.
		 */
		
        Map<Object, Object> map = new HashMap<>();
        map.put("message", "string");
		log.info("Log message 10 " + counter2++, StructuredArguments.entries(map));
	}

	@Scheduled(cron = "20 * * * * *")
	public void scheduledEvery20Second() {
		
		/**
		 * Lose about 2-4 seconds of logs
		 * 
		 * Fluentbit: Failed to get record: decoder: failed to decode payload: msgpack decode error [pos 757]: runtime error: hash of unhashable type []interface {}
		 */
		
		HashMap<Object,Object> hashMap = new HashMap<>();
		hashMap.put("key", "value");
		
		Map<Object, Object> map = new HashMap<>();
		map.put("message", hashMap);
		log.info("Log message 20 " + counter3++, StructuredArguments.entries(map));
	}

	@Scheduled(cron = "30 * * * * *")
	public void scheduledEvery30Second() {
		/**
		 * Lose about 2-4 seconds of logs
		 * 
		 * Fluentbit: Failed to get record: decoder: failed to decode payload: msgpack decode error [pos 498]: cannot decode signed integer: unrecognized descriptor byte: b0/string|bytes
		 */
		Map<Object, Object> map = new HashMap<>();
		map.put("message", 1);
		log.info("Log message 30 " + counter4++, StructuredArguments.entries(map));
	}
	
	@Scheduled(cron = "40 * * * * *")
	public void scheduledEvery40Second() {
		// no impact
		HashMap<Object,Object> hashMap = new HashMap<>();
		hashMap.put("message", "value");
		
		Map<Object, Object> map = new HashMap<>();
		map.put("myKey40", hashMap);
		log.info("Log message 40 " + counter5++, StructuredArguments.entries(map));
	}
	
	@Scheduled(cron = "50 * * * * *")
	public void scheduledEvery50Second() {
		/**
		 * Lose about 2-4 seconds of logs
		 * 
		 * Failed to get record: decoder: failed to decode payload: msgpack decode error [pos 717]: cannot decode unsigned integer: unrecognized descriptor byte: a7/string|bytes
		 */
		Map<Object, Object> map = new HashMap<>();
		map.put("timestamp", "abcde");
		log.info("Log message 50 " + counter6++, StructuredArguments.entries(map));
	}
```
</details>

Resolution: Apps should put their app-specific fields in a subtree rather than on the root.

**Local/test-scope enforcement:** the GCP test-scope module (`gcp/spring-boot-autoconfigure-gcp-test`) wires `no.entur.logging.cloud.gcp.spring.test.GcpLogStatementFieldNameValidator` onto `CompositeConsoleAsyncAppenderLogging` (see `logback-test.xml`), which runs `no.entur.logging.cloud.gcp.spring.test.LogStatementFieldNameValidator` synchronously, on the calling thread, for every log statement - right after it has been handed off to the async appender. It inspects the event's MDC, the logback context properties (which the encoder also writes at the root), its SLF4J key/value pairs (`addKeyValue`) and the logstash-logback `Marker`/`StructuredArgument` types (`Markers`/`StructuredArguments`, including `fields`/`appendFields`, and custom `LogstashMarker`s) used to attach fields, and throws `IllegalStateException` immediately if a field name is either reserved by one of the encoder's built-in providers (e.g. `message`, `severity`, `timestamp`) or repeated - so this class of mistake blows up right where the offending log statement was made, instead of surfacing later as dropped production logs (or on a background worker thread, far from the log call site). The production `StackdriverLogstashEncoder` itself is unaffected; this check only runs in test scope.

**Azure:** the Azure test-scope module (`azure/spring-boot-autoconfigure-azure-test`) is wired the same way (`no.entur.logging.cloud.azure.spring.test.AzureLogStatementFieldNameValidator`, see its `logback-test.xml`), but, mirroring what the Azure encoder actually writes, only `LogstashMarker`s (`Markers.append`, `appendEntries`, `appendFields` and custom ones) are checked, against the Azure encoder's own reserved names (`timestamp`, `logger`, `level`, `thread`, `mdc`, `message`, `stackTrace`, `exceptionClass`, `serviceContext`). The MDC is nested in an `mdc` object, and structured arguments and key/value pairs only end up in the message, so none of those can collide with a root-level field.

Things to be aware of:

 * The offending log statement is still written - it is the call site which fails. As the exception propagates through logback's appender loop, any appender attached to the same logger *after* the async console appender does not receive the statement.
 * Only field names are checked, on a best-effort basis: values are never serialized, deferred arguments/markers (`StructuredArguments.defer`, `Markers.defer`) are not resolved, and an object unwrapped by `fields`/`appendFields` which cannot be serialized by a plain Jackson mapper is skipped.
 * The check can be switched off (in either module) with the system property `entur.logging.test.field-name-validation.enabled=false`, for example while a legacy code base (or third-party code) is being cleaned up. With Gradle: `test { systemProperty 'entur.logging.test.field-name-validation.enabled', 'false' }`.

### Invalid JSON
Does not always translate into `textPayload`.

> Received empty or invalid msgpack for tag kube_abt-xxx_abt-xxx-746cbbdb87-tnf54_abt-xxx: decoder: failed to decode payload: msgpack decode error [pos 1769]: runtime error: hash of unhashable type map[interface {}]interface {}"

### Too long lines
Does not always translate into `textPayload`.

> Failed to process request with tag kube_abt-xxx_abt-ccc-79d5974b67-jdsk8_abt-ccc_stdout: rpc error: code = InvalidArgument desc = Log entry with size 278.8K exceeds maximum size of 256.0K

Resolution: Truncate long values. For request-response logging, the library automatically truncates request/response bodies at 200 KiB by default (GCP `LogEntry` max is 256 KiB). The limit can be lowered further via `entur.logging.request-response.max-body-size`.