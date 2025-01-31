package no.entur.logging.cloud.trace.spring.web;

import org.junit.jupiter.api.Test;

import static no.entur.logging.cloud.trace.spring.web.CorrelationIdMdcSupportBuilder.containsNumbersLowercaseLettersAndDashes;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CorrelationIdMdcSupportBuilderTest {

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
