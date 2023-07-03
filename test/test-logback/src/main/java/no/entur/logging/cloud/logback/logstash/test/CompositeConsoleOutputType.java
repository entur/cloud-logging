package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.core.encoder.Encoder;

public enum CompositeConsoleOutputType {

    humanReadablePlainEncoder,
    humanReadableJsonEncoder,

    machineReadableJsonEncoder;

}
