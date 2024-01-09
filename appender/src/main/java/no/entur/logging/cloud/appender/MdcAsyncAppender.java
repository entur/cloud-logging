package no.entur.logging.cloud.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.MDC;

import java.util.Map;

public class MdcAsyncAppender extends AsyncAppender {

    protected MdcContributer mdcContributer = new MdcContributer(){};

    public void setMdcContributer(MdcContributer mdcContributer) {
        this.mdcContributer = mdcContributer;
    }

    @Override
    public void preprocess(ILoggingEvent eventObject) {
        // capture gRPC MDC context, if any
        Map<String, String> mdc = mdcContributer.getMdc();
        if (mdc.isEmpty()) {
            super.preprocess(eventObject);
        } else {
            for (Map.Entry<String, String> entry : mdc.entrySet()) {
                MDC.put(entry.getKey(), entry.getValue());
            }
            try {
                super.preprocess(eventObject);
            } finally {
                for (Map.Entry<String, String> entry : mdc.entrySet()) {
                    MDC.remove(entry.getKey());
                }
            }
        }
    }

}
