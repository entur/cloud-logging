# logbook-filter
Various request/response/body filters for Logbook:

 * max body size filter
 * max string field value body filter

with

 * per-path request and/or response body filter selection based on path

## JSON filtering

```
@Bean
public JsonOnlyMatchPathStrategy strategy() {
  return new JsonOnlyMatchPathStrategy();
}
```

with request filter

```
@Bean
public RequestFilter myRequestFilter() {
  JacksonJsonFieldBodyFilter customerRequestFilter = new JacksonJsonFieldBodyFilter(Arrays.asList("name"), "XXX");
  return RequestMatcherRequestFilter.newPathPrefixBuilder().withPathPrefixFilter("/api/customer", customerRequestFilter).build();
}
```

and response filter

```
@Bean
public ResponseFilter myResponseFilter() {
  JacksonJsonFieldBodyFilter customerResponseFilter = new JacksonJsonFieldBodyFilter(Arrays.asList("rating"), "XXX");
  return RequestResponseMatcherResponseFilter.newPathPrefixBuilder().withPathPrefixFilter("/api/customer", customerResponseFilter).build();
}
```

### Advanced filtering
Construct your own `PathFilterMatcher` to handle more complex filtering, i.e.

 * path 
 * max size
 * origin

using 

 * SizePathFilterMatcher
 * SizeOriginPathFilterMatcher

