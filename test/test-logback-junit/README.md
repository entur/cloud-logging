# logback test
Composite appender with room for three encoders.

# human-readable (for local development)
The first two encoders are for printing to console:

- 'classic' one-line logging
  - color coded
- 'friendly' JSON logging
  - pretty-printed,
  - color coded, and
  - with fewer default fields

# machine-readable (for unit testing)
The last encoder is for unit testing of logging, i.e. machine-readable JSON output.

## Known issues
Can not yet assert against MDC fields set in gRPC MDC context.

When using an async appender, there is a race condition for in-flight log events when switching from one encoder to another.