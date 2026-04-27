package no.entur.logging.cloud.gcp.logback.logstash;

import no.entur.logging.cloud.api.DevOpsLevel;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

public class StackdriverSeverityTest {

    @Test
    void forDevOpsLevel_errorTellMeTomorrow_mapsToError() {
        assertThat(StackdriverSeverity.forDevOpsLevel(DevOpsLevel.ERROR_TELL_ME_TOMORROW))
                .isEqualTo(StackdriverSeverity.ERROR);
    }

    @Test
    void forDevOpsLevel_errorInterruptMyDinner_mapsToCritical() {
        assertThat(StackdriverSeverity.forDevOpsLevel(DevOpsLevel.ERROR_INTERRUPT_MY_DINNER))
                .isEqualTo(StackdriverSeverity.CRITICAL);
    }

    @Test
    void forDevOpsLevel_errorWakeMeUpRightNow_mapsToAlert() {
        assertThat(StackdriverSeverity.forDevOpsLevel(DevOpsLevel.ERROR_WAKE_ME_UP_RIGHT_NOW))
                .isEqualTo(StackdriverSeverity.ALERT);
    }

    @Test
    void isGreaterOrEqual_errorGreaterThanWarning_isTrue() {
        assertThat(StackdriverSeverity.ERROR.isGreaterOrEqual(StackdriverSeverity.WARNING)).isTrue();
    }

    @Test
    void isGreaterOrEqual_infoEqualToInfo_isTrue() {
        assertThat(StackdriverSeverity.INFO.isGreaterOrEqual(StackdriverSeverity.INFO)).isTrue();
    }

    @Test
    void isGreaterOrEqual_debugLessThanWarning_isFalse() {
        assertThat(StackdriverSeverity.DEBUG.isGreaterOrEqual(StackdriverSeverity.WARNING)).isFalse();
    }
}
