# protobuf-json

Writes protobuf messages as JSON to a Jackson 3 `JsonGenerator` (or a `String`), using the
[ProtoJSON format](https://protobuf.dev/programming-guides/json/) - the same output as
`JsonFormat.printer().omittingInsignificantWhitespace()`.

```java
ProtobufJsonWriter writer = TranscodingProtobufJsonWriter.newBuilder()
        .withTypeRegistry(typeRegistry) // for google.protobuf.Any
        .build();

writer.write(message, generator);
String json = writer.writeAsString(message);
```

## Implementations

| Implementation | Package | Description |
|---|---|---|
| `JsonFormatProtobufJsonWriter` | `jsonformat` | `JsonFormat` (default) |
| `TranscodingProtobufJsonWriter` | `transcode` | serializes to bytes, then transcodes the bytes to JSON |
| `CodedOutputStreamProtobufJsonWriter` | `codedoutputstream` | the generated serialization code writes JSON directly, via a JSON-writing `CodedOutputStream` (Java 24+) |

`JsonFormat` reads every field via reflection (boxing values, building a map of fields per message) and appends
many small strings. The other two let the message's generated serialization code drive the output:

 * **Transcoding**: the message is serialized to bytes, which are then read field by field and written as JSON.
   Uses only public protobuf APIs.
 * **CodedOutputStream**: the generated `writeTo(CodedOutputStream)` writes to a `CodedOutputStream` which produces
   JSON rather than the binary format. `CodedOutputStream` only has a private constructor, so the subclass is generated
   with the ClassFile API and defined as a hidden class in `CodedOutputStream`'s nest
   (`MethodHandles.privateLookupIn` + `Lookup.ClassOption.NESTMATE`). This depends on how protobuf's generated code
   calls `CodedOutputStream`. Falls back to transcoding for `DynamicMessage`, on Java versions before 24, and if the
   hidden class cannot be defined (i.e. if protobuf is loaded by a class loader which cannot see this module),
   see `CodedOutputStreamProtobufJsonWriter.isAvailable()`.

Both use a per-message-type plan (package `plan`; field names, enum names, well-known types), indexed by field number.
Strings are written as UTF-8 without decoding, where possible.

The module targets Java 17; it is a multi-release jar, with the ClassFile API code in `src/main/java24`.

**Note:** if the jar is repackaged, i.e. shaded or merged into an uber jar, the repackaged jar's manifest must contain
`Multi-Release: true`, otherwise the Java 24+ classes are ignored and `CodedOutputStreamProtobufJsonWriter` silently
falls back to transcoding (`CodedOutputStreamProtobufJsonWriter.getUnavailableCause()` explains why). Spring Boot
executable jars keep dependencies as nested jars, so they are not affected. For the Maven Shade Plugin, add the
attribute with a `ManifestResourceTransformer`; for Gradle `Jar` tasks, use `manifest { attributes('Multi-Release': 'true') }`.

### Performance
Time and allocation per message, written to a byte-based JSON generator (JMH, Java 25):

| Payload | JsonFormat | Transcoding | CodedOutputStream |
|---|---|---|---|
| small (~100 bytes) | 4.6 µs / 7.5 KB | 1.0 µs / 2.8 KB | 1.0 µs / 2.8 KB |
| medium (all field types, maps, well-known types) | 177 µs / 430 KB | 61 µs / 113 KB | 49 µs / 82 KB |
| large (20 KB, many strings and nested messages) | 128 µs / 366 KB | 47 µs / 50 KB | 34 µs / 0.6 KB |

### Differences from JsonFormat (transcoding and CodedOutputStream)

 * extensions are not written
 * out of range `google.protobuf.Timestamp` and `google.protobuf.Duration` values are written as objects, rather than failing
 * NaN and infinity `google.protobuf.Value` numbers are written as strings, rather than failing

No JSON key is written twice:

 * fields are expected in increasing field number order, as protobuf serializes them; a field number seen again after
   a higher one (i.e. in the unknown fields) is skipped.
 * map entries with a key already written are skipped, keeping the first entry (protobuf keeps the last when
   parsing). Generated messages never have duplicate keys, but i.e. a `DynamicMessage` can; `JsonFormat` then writes
   the key twice.

## Checking the output
Use `JsonFormatComparison` to check that an implementation writes the same as `JsonFormat` for your messages,
i.e. in a unit test:

```java
JsonFormatComparison comparison = JsonFormatComparison.newBuilder()
        .withTypeRegistry(typeRegistry)
        .withWriter(writer)
        .build();

comparison.verify(myMessage); // throws IllegalStateException with the differences
```

Outputs are compared as JSON values, so escaping and number notation do not matter (`JsonFormat` escapes i.e. `=`
as `\u003d`). Duplicate properties are reported.

## Tests
`JsonFormatConformanceTest` compares the output with `JsonFormat` for random messages of the protobuf conformance
test types (`src/test/proto/conformance`, copied from protobuf v36.1). Tests run on Java 25.

## Benchmarks
```
./gradlew :request-response:protobuf-json:jmh
```

Optionally with JMH arguments, i.e.

```
./gradlew :request-response:protobuf-json:jmh -Pjmh="-prof gc ProtobufJsonWriterBenchmark"
```
