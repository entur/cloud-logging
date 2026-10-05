package no.entur.logging.cloud.protobuf.json;

import tools.jackson.core.json.JsonFactory;

final class ProtobufJsonWriterSupport {

    static final JsonFactory JSON_FACTORY = JsonFactory.builder().build();

    private ProtobufJsonWriterSupport() {
    }
}
