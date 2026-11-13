package no.entur.logging.cloud.logback.logstash.test.ai;

import ch.qos.logback.core.PropertyDefinerBase;

/**
 * The pid of this JVM, for the names of the AI files in <code>logback-ai.xml</code>: every test JVM writes its own compact and full
 * file (<code>&lt;file&gt;.&lt;pid&gt;.compact</code>), so that test JVMs running at the same time do not write into each other's lines.
 */
public class AiPidPropertyDefiner extends PropertyDefinerBase {

    @Override
    public String getPropertyValue() {
        return Long.toString(ProcessHandle.current().pid());
    }
}
