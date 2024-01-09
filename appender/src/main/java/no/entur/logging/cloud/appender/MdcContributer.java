package no.entur.logging.cloud.appender;

import java.util.Collections;
import java.util.Map;

public interface MdcContributer {

    default Map<String, String> getMdc() {
        return Collections.emptyMap();
    }

}
