package no.entur.logging.cloud.logback.logstash.test.ai.common;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import no.entur.logging.cloud.logback.logstash.test.ai.AiTestThread;

/**
 * The test the event belongs to, as <code> #t17</code>, or the tests it may belong to, as <code> ~ #t17 #t21</code> (empty if none),
 * see {@link AiTestThread}.
 */
public class AiTestConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String tag = AiTestThread.tagOf(event);
        return tag == null ? "" : " " + tag;
    }
}
