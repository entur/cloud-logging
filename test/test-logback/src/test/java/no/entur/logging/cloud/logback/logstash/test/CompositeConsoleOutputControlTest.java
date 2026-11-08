package no.entur.logging.cloud.logback.logstash.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CompositeConsoleOutputControlTest {

    @AfterEach
    public void restoreValidation() {
        CompositeConsoleOutputControl.setValidationEnabled(true);
    }

    @Test
    public void testValidationIsEnabledByDefault() {
        assertThat(CompositeConsoleOutputControl.isValidationEnabled()).isTrue();
    }

    @Test
    public void testSetValidationEnabled() {
        CompositeConsoleOutputControl.setValidationEnabled(false);
        assertThat(CompositeConsoleOutputControl.isValidationEnabled()).isFalse();

        CompositeConsoleOutputControl.setValidationEnabled(true);
        assertThat(CompositeConsoleOutputControl.isValidationEnabled()).isTrue();
    }

    @Test
    public void testDisableValidationRestoresPreviousStateOnClose() {
        try (CompositeConsoleValidationControlClosable c = CompositeConsoleOutputControl.disableValidation()) {
            assertThat(CompositeConsoleOutputControl.isValidationEnabled()).isFalse();
        }
        assertThat(CompositeConsoleOutputControl.isValidationEnabled()).isTrue();
    }

    @Test
    public void testNestedDisableValidationRestoresDisabledState() {
        try (CompositeConsoleValidationControlClosable outer = CompositeConsoleOutputControl.disableValidation()) {
            try (CompositeConsoleValidationControlClosable inner = CompositeConsoleOutputControl.disableValidation()) {
                assertThat(CompositeConsoleOutputControl.isValidationEnabled()).isFalse();
            }
            // still disabled by the outer scope
            assertThat(CompositeConsoleOutputControl.isValidationEnabled()).isFalse();
        }
        assertThat(CompositeConsoleOutputControl.isValidationEnabled()).isTrue();
    }

    @Test
    public void testDisableValidationRestoresPreviouslyDisabledState() {
        CompositeConsoleOutputControl.setValidationEnabled(false);

        try (CompositeConsoleValidationControlClosable c = CompositeConsoleOutputControl.disableValidation()) {
            assertThat(CompositeConsoleOutputControl.isValidationEnabled()).isFalse();
        }
        assertThat(CompositeConsoleOutputControl.isValidationEnabled()).isFalse();
    }

    @Test
    public void testDisableValidationConsumerRestoresStateEvenWhenItThrows() {
        CompositeConsoleOutputControl.Consumer consumer = () -> {
            assertThat(CompositeConsoleOutputControl.isValidationEnabled()).isFalse();
            throw new IllegalStateException("boom");
        };

        assertThrows(IllegalStateException.class, () -> CompositeConsoleOutputControl.disableValidation(consumer));

        assertThat(CompositeConsoleOutputControl.isValidationEnabled()).isTrue();
    }

    @Test
    public void testDisableValidationCallableReturnsValueAndRestoresState() throws Exception {
        String result = CompositeConsoleOutputControl.disableValidation(() -> {
            assertThat(CompositeConsoleOutputControl.isValidationEnabled()).isFalse();
            return "value";
        });

        assertThat(result).isEqualTo("value");
        assertThat(CompositeConsoleOutputControl.isValidationEnabled()).isTrue();
    }
}
