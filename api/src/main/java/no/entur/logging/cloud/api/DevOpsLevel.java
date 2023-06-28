package no.entur.logging.cloud.api;

public enum DevOpsLevel {

    TRACE,
    DEBUG,
    INFO,

    @Deprecated
    ERROR,
    ERROR_TELL_ME_TOMORROW,
    ERROR_INTERRUPT_MY_DINNER,
    ERROR_WAKE_ME_UP_RIGHT_NOW
}
