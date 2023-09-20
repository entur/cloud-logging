Note: Work in progress

# Logging for GRPC

Request-response-logging for GRPC.

> Request-response-logging is intended for using in unit testing and test environments.

### Request-response-logging

gRPC interceptors support the following steps:

* `connect`
* `request` (1 or more)
* `response` (1 or more)
* `disconnect`

Note that the `disconnect` is described as "summary" in the log if no corresponding `connect` has been logged.

The (per-path) supported logging modes are

| Mode | Description | 
| ---- | ------- | 
| Full | All steps | 
| Classic | Just `request` / `response` steps, i.e. not `connect` / `disconnect`. |
| Summary | Just `disconnect` step |
| No | no | None |

The only built-in path matcher is a prefix matcher, supply a custom `GrpcLogFilterPathMatcher` for more advanced
matching, i.e. using Spring's Antmatcher.

## Example output

Connect

```json
{
  "@timestamp": "2020-03-30T19:22:06.937+02:00",
  "@version": "1",
  "message": "Connect /org.entur.oidc.grpc.test.GreetingService/greeting1",
  "logger_name": "org.entur.logging.org.entur.logging.slf4jv20.GrpcServerLoggingInterceptor",
  "thread_name": "grpc-default-executor-1",
  "severity": "DEBUG",
  "level_value": 10000,
  "requestId": "d7960631-53ba-4d9b-b3e0-2e50513de0ab",
  "correlationId": "81658426-3de0-4024-b99d-3fd5b166c412",
  "http": {
    "uri": "/org.entur.oidc.grpc.test.GreetingService/greeting1",
    "type": "connect",
    "remote": "127.0.0.1",
    "origin": "remote",
    "headers": {
      "content-type": [
        "application/grpc"
      ],
      "grpc-accept-encoding": [
        "gzip"
      ],
      "user-agent": [
        "grpc-java-netty/1.27.1"
      ]
    }
  }
}
```


Request

```json
{
  "@timestamp": "2020-03-30T19:22:06.937+02:00",
  "@version": "1",
  "message": "Request #1 GET /org.entur.oidc.grpc.test.GreetingService/greeting1",
  "logger_name": "org.entur.logging.org.entur.logging.slf4jv20.GrpcServerLoggingInterceptor",
  "thread_name": "grpc-default-executor-1",
  "severity": "DEBUG",
  "level_value": 10000,
  "requestId": "d7960631-53ba-4d9b-b3e0-2e50513de0ab",
  "correlationId": "81658426-3de0-4024-b99d-3fd5b166c412",
  "http": {
    "method": "GET",
    "uri": "/org.entur.oidc.grpc.test.GreetingService/greeting1",
    "type": "request",
    "remote": "127.0.0.1",
    "origin": "remote",
    "headers": {
      "content-type": [
        "application/grpc"
      ],
      "grpc-accept-encoding": [
        "gzip"
      ],
      "user-agent": [
        "grpc-java-netty/1.27.1"
      ]
    }
  }
}
```

Successful response:

```json
{
  "@timestamp": "2020-03-30T19:22:06.937+02:00",
  "@version": "1",
  "message": "Response #1 200 OK /org.entur.oidc.grpc.test.GreetingService/greeting1",
  "logger_name": "org.entur.logging.org.entur.logging.slf4jv20.GrpcServerLoggingInterceptor",
  "thread_name": "grpc-default-executor-1",
  "severity": "DEBUG",
  "level_value": 10000,
  "requestId": "d7960631-53ba-4d9b-b3e0-2e50513de0ab",
  "correlationId": "81658426-3de0-4024-b99d-3fd5b166c412",
  "http": {
    "status": 200,
    "uri": "/org.entur.oidc.grpc.test.GreetingService/greeting1",
    "type": "response",
    "remote": "127.0.0.1",
    "origin": "local",
    "headers": {
      "grpc-encoding": [
        "identity"
      ],
      "grpc-accept-encoding": [
        "gzip"
      ]
    },
    "body": {
      "status": "100",
      "message": "Hello"
    }
  }
}
```

Disconnect response:

```json
{
  "@timestamp": "2020-03-30T19:13:59.350+02:00",
  "@version": "1",
  "message": "Disconnect #1 /org.entur.oidc.grpc.test.GreetingService/greeting1",
  "logger_name": "org.entur.logging.org.entur.logging.slf4jv20.GrpcServerLoggingInterceptor",
  "thread_name": "grpc-default-executor-1",
  "severity": "DEBUG",
  "level_value": 10000,
  "requestId": "5c28bc75-d55b-4663-9e6d-1ec9447adf8f",
  "correlationId": "5373bf7a-4ac8-43cf-a09b-79b94a68a883",
  "http": {
    "uri": "/org.entur.oidc.grpc.test.GreetingService/greeting1",
    "type": "disconnect",
    "remote": "127.0.0.1",
    "origin": "local",
    "headers": {
      "grpc-status": [
        "0"
      ]
    }
  }
}
```
