package no.entur.logging.cloud.spring.ondemand.web.scope;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

public class HttpStatusAtLeastPredicateTest {

    @Test
    public void testEqualToLimit() {
        assertThat(new HttpStatusAtLeastPredicate(400).test(400)).isTrue();
    }

    @Test
    public void testAboveLimit() {
        assertThat(new HttpStatusAtLeastPredicate(400).test(500)).isTrue();
    }

    @Test
    public void testJustBelowLimit() {
        assertThat(new HttpStatusAtLeastPredicate(400).test(399)).isFalse();
    }

    @Test
    public void testWellBelowLimit() {
        assertThat(new HttpStatusAtLeastPredicate(400).test(200)).isFalse();
    }
}
