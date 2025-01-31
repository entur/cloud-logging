package no.entur.logging.cloud.appender.scope;

/**
 *
 * Interface for performing additional work on log event markers if they are to be logged (not dropped).
 *
 * This work will be performed on the same thread which collected the log events, i.e. or at least a "worker" thread.
 *
 * In other words, this interface is intended to offload heavy work from the async logging thread.
 *
 */

public interface LoggingScopePostProcessing {

    void performPostProcessing();

}
