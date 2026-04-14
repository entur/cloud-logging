package no.entur.logging.cloud.spring.ondemand.web.scope;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

public class HttpHeaderPresentPredicateTest {

    private final HttpHeaderPresentPredicate predicate = new HttpHeaderPresentPredicate(Set.of("X-Request-Id", "X-Correlation-Id"));

    @Test
    public void testMatchFirstHeader() {
        assertThat(predicate.test(Collections.enumeration(Collections.singletonList("X-Request-Id")))).isTrue();
    }

    @Test
    public void testMatchSecondHeader() {
        assertThat(predicate.test(Collections.enumeration(Collections.singletonList("X-Correlation-Id")))).isTrue();
    }

    @Test
    public void testMatchAmongMultipleHeaders() {
        assertThat(predicate.test(Collections.enumeration(List.of("Accept", "X-Request-Id", "Content-Type")))).isTrue();
    }

    @Test
    public void testNoMatchAbsentHeader() {
        assertThat(predicate.test(Collections.enumeration(Collections.singletonList("Authorization")))).isFalse();
    }

    @Test
    public void testNoMatchEmptyEnumeration() {
        assertThat(predicate.test(Collections.emptyEnumeration())).isFalse();
    }
}
