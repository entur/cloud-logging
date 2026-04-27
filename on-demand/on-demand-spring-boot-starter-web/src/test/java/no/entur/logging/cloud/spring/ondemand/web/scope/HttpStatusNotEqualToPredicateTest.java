package no.entur.logging.cloud.spring.ondemand.web.scope;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class HttpStatusNotEqualToPredicateTest {

    private final HttpStatusNotEqualToPredicate predicate = new HttpStatusNotEqualToPredicate(List.of(200, 201));

    @Test
    public void testExcludedFirstCode() {
        assertThat(predicate.test(200)).isFalse();
    }

    @Test
    public void testExcludedSecondCode() {
        assertThat(predicate.test(201)).isFalse();
    }

    @Test
    public void testNotExcludedWithinRange() {
        assertThat(predicate.test(404)).isTrue();
    }

    @Test
    public void testNotExcludedBeyondRange() {
        assertThat(predicate.test(999)).isTrue();
    }
}
