/**
 * AI mode: test logging written to a compact and a full file instead of the console (see the README of this module). This package
 * switches the files on and writes them; the other modules use {@link no.entur.logging.cloud.logback.logstash.test.ai.AiLoggingControl}
 * and {@link no.entur.logging.cloud.logback.logstash.test.ai.AiBodyMarker}.
 * <p>
 * Subpackages: <code>common</code> has what both files use, <code>compact</code> and <code>full</code> what only one of them uses.
 * Dependencies go upwards only: <code>compact</code> and <code>full</code> use <code>common</code> and this package,
 * <code>common</code> uses this package, and <code>compact</code> and <code>full</code> do not use each other.
 */
package no.entur.logging.cloud.logback.logstash.test.ai;
