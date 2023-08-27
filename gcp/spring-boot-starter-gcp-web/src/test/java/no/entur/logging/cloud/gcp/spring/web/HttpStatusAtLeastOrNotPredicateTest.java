package no.entur.logging.cloud.gcp.spring.web;

import no.entur.logging.cloud.gcp.spring.web.scope.HttpStatusAtLeastOrNotPredicate;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpStatusAtLeastOrNotPredicateTest {

    @Test
    public void testNot() {
        // true means extra logging should be triggered (positive failure)

        java.util.List<Integer> include = Arrays.asList(300);
        List<Integer> exclude = Arrays.asList(500);
        HttpStatusAtLeastOrNotPredicate notPredicate = new HttpStatusAtLeastOrNotPredicate(400, include, exclude);

        // regular cases
        assertFalse(notPredicate.test(200));
        assertTrue(notPredicate.test(404));

        // explicit cases
        assertFalse(notPredicate.test(500));
        assertTrue(notPredicate.test(300));

    }
}
