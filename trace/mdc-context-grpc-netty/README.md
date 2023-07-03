# MDC Context Logging for gRPC
MDC call context with projection for gRPC.

Requires use of a custom Async Appender `org.entur.logging.grpc.mdc.GrpcMdcContextAsyncAppender` in the log configuration:

```xml
<configuration>
    <appender name="STDOUT_JSON" class="ch.qos.logback.core.ConsoleAppender">
		<encoder class="net.logstash.logback.encoder.LogstashEncoder">
		</encoder>
    </appender>

	<appender name="STDOUT_JSON_ASYNC" class="org.entur.logging.grpc.mdc.GrpcMdcContextAsyncAppender">
		<discardingThreshold>0</discardingThreshold>
		<maxFlushTime>0</maxFlushTime>
		<appender-ref ref="STDOUT_JSON" />
	</appender>

	<logger name="io.grpc.netty" severity="WARN"/>
	<logger name="io.netty" severity="INFO"/>

	<root severity="DEBUG">
		<appender-ref ref="STDOUT_JSON_ASYNC" />
	</root>
</configuration>
```

A custom class must be used to populate the context. Create MDC Context using

```java
GrpcMdcContext grpcMdcContext = new GrpcMdcContext();
Context context = Context.current().withValue(GrpcMdcContext.KEY_CONTEXT, grpcMdcContext);
```

then later set/clear values using

```
GrpcMdcContext grpcMdcContext = GrpcMdcContext.get();
try {
  grpcMdcContext.put("myKey", "myValue");
    
  // ...
} finally {
  grpcMdcContext.remove("myKey");
}
```

