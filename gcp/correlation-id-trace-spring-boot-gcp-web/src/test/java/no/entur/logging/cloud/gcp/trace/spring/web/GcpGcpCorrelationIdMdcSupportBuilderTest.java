package no.entur.logging.cloud.gcp.trace.spring.web;

import org.junit.jupiter.api.Test;

import static no.entur.logging.cloud.gcp.trace.spring.web.GcpCorrelationIdMdcSupportBuilder.containsNumbersLowercaseLettersAndDashes;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GcpGcpCorrelationIdMdcSupportBuilderTest {

    @Test
    public void testPositive() {
        assertTrue(containsNumbersLowercaseLettersAndDashes("abc-def"));
    }

    @Test
    public void testNegative() {
        assertFalse(containsNumbersLowercaseLettersAndDashes("!"));
        assertFalse(containsNumbersLowercaseLettersAndDashes("%"));
        assertFalse(containsNumbersLowercaseLettersAndDashes("AZ"));
    }
}
