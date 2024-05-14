# appender
This module contains

 * Extendable appenders with customized MDC support.
 * Logging scopes in support of on-demand logging

## On-demand logging
Enable "on-demand" logging for unexpected web server behaviour:

  * Caches log statements for each request in memory, then
  * throws them away for successful responses, or
  * logs them in case of
      * failed responses (i.e. HTTP status code >= 400) and/or
      * log events of a certain level (i.e. warning or error) was made
      * troubleshooting flag passed a header

Advantages:

  * reduced logging for happy cases
  * more "sibling" log statements for non-happy cases (i.e. not just WARN or ERROR log statements)
  * reduces the amount of work necessary to guarantee well-formed JSON log statements for request-response logging
      * skips JSON syntax check for throw-away request-response log statements, and/or
      * piggybacks on Spring REST framework databinding JSON syntax check 

