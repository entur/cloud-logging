package no.entur.logging.cloud.logbook;

import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;

public class DefaultRemoteHttpMessageContextSupplier implements RemoteHttpMessageContextSupplier {

    @Override
    public boolean verifyJsonSyntax(HttpRequest message) {
        return true;
    }

    @Override
    public boolean verifyJsonSyntax(HttpResponse message) {
        return true;
    }

}
