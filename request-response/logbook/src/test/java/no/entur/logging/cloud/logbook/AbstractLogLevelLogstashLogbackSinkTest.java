package no.entur.logging.cloud.logbook;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AbstractLogLevelLogstashLogbackSinkTest {

    @Test
    public void test() {
        Assertions.assertTrue(AbstractLogLevelLogstashLogbackSink.isXmlMediaType("text/xml"));
        Assertions.assertTrue(AbstractLogLevelLogstashLogbackSink.isXmlMediaType("application/xml"));

        Assertions.assertTrue(AbstractLogLevelLogstashLogbackSink.isXmlMediaType("text/xml;charset=UTF-8"));
        Assertions.assertTrue(AbstractLogLevelLogstashLogbackSink.isXmlMediaType("application/xml;charset=UTF-8"));

        Assertions.assertTrue(AbstractLogLevelLogstashLogbackSink.isXmlMediaType("application/ehf+xml"));
        Assertions.assertTrue(AbstractLogLevelLogstashLogbackSink.isXmlMediaType("application/vnd.difi.dpi.lenke+xml"));

        Assertions.assertFalse(AbstractLogLevelLogstashLogbackSink.isXmlMediaType("application/json"));
    }

}
