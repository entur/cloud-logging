package no.entur.logging.cloud.appender.scope;

import java.util.Map;

public interface MdcLoggingScope extends LoggingScope {

    Map<String, String> getMdcContext();
}
