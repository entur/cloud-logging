package no.entur.logging.cloud.gcp.spring.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;

import java.io.OutputStream;

/**
 * Discards all events. Only exists to expose a (JSON) encoder so that test extensions can parse log statements in AI mode.
 */
public class NoopAppender extends OutputStreamAppender<ILoggingEvent> {

    @Override
    public void start() {
        setOutputStream(OutputStream.nullOutputStream());
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        // intentionally empty
    }
}
