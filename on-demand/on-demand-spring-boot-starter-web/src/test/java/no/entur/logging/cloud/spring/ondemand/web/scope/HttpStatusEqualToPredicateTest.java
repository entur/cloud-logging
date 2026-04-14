package no.entur.logging.cloud.spring.ondemand.web.scope;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class HttpStatusEqualToPredicateTest {

    private final HttpStatusEqualToPredicate predicate = new HttpStatusEqualToPredicate(List.of(404, 500));

    @Test
    public void testMatchFirstCode() {
        assertThat(predicate.test(404)).isTrue();
    }

    @Test
    public void testMatchSecondCode() {
        assertThat(predicate.test(500)).isTrue();
    }

    @Test
    public void testNoMatchSuccess() {
        assertThat(predicate.test(200)).isFalse();
    }

    @Test
    public void testNoMatchBeyondArrayLength() {
        assertThat(predicate.test(999)).isFalse();
    }

    @Test
    public void testNoMatchWithinRange() {
        assertThat(predicate.test(403)).isFalse();
    }
}
