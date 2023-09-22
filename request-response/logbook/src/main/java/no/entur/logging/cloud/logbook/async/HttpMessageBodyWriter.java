package no.entur.logging.cloud.logbook.async;

import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;

/**
 *
 * This interface aims to facilitate inline JSON payload logging for request-response log statements.
 * Goals:<br><br>
 *
 *  - do as little work as possible<br>
 *  - ensure the resulting JSON log statement has correct syntax and size.<br>
 *  - for async logging, do as little works as possible on the logging thread<br>
 *
 */

public interface HttpMessageBodyWriter {

    void prepareWriteBody();

    void writeBody(JsonGenerator generator) throws IOException;

}
