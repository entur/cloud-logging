# troubleshooting
# GCP
Fluentbit is the service which translates console logging into Stackdriver (AKA cloud logging) log entries.
## Fluentbit errors

### Duplicate `JsonPayload` field names
`Fluentbit` blows up if `JsonPayload` fields are duplicated. Symptoms:

 * truncated text values
 * **missing logs for 2-4 seconds**
   * Non-related log statements are also lost. 

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
		 * Loose about 2-4 seconds of logs
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
		 * Loose about 2-4 seconds of logs
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
		 * Loose about 2-4 seconds of logs
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

### Invalid JSON
Does not always translate into `TextPayload`.

> Received empty or invalid msgpack for tag kube_abt-xxx_abt-xxx-746cbbdb87-tnf54_abt-xxx: decoder: failed to decode payload: msgpack decode error [pos 1769]: runtime error: hash of unhashable type map[interface {}]interface {}"

Resolution: Check use of `RawAppendingMarker` and so on. 
### Too long lines
Does not always translate into `TextPayload`.

> Failed to process request with tag kube_abt-xxx_abt-ccc-79d5974b67-jdsk8_abt-ccc_stdout: rpc error: code = InvalidArgument desc = Log entry with size 278.8K exceeds maximum size of 256.0K

Resolution: Truncate long values.