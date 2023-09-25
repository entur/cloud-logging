package no.entur.logging.cloud.logbook;

import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;


/**
 *
 * Support for skipping verification of certain requests or responses.
 *
 * So this makes it possible to trust authenticated users or our own services to respond with data which
 * has valid JSON syntax, simplifying the process of writing a valid JSON log statement.
 */

public interface RemoteHttpMessageContextSupplier {

    boolean verifyJsonSyntax(HttpRequest message);

    boolean verifyJsonSyntax(HttpResponse message);

}
