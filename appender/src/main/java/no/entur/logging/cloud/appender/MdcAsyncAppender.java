package no.entur.logging.cloud.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

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
            // make sure the original MDC values are left after this operation

            // this only works if there is a single appender, i.e.
            // ILoggingEvent#prepareForDeferredProcessing() has not been called
            MDCAdapter mdcAdapter = MDC.getMDCAdapter();
            for (Map.Entry<String, String> entry : mdc.entrySet()) {
                String key = entry.getKey();
                if(key == null) {
                    continue;
                }
                String value = entry.getValue();
                if(value == null) {
                    continue;
                }
                mdcAdapter.put(key, value);
            }
            try {
                super.preprocess(eventObject);
            } finally {
                for (Map.Entry<String, String> entry : mdc.entrySet()) {
                    String key = entry.getKey();
                    if(key == null) {
                        continue;
                    }
                    mdcAdapter.remove(key);
                }
            }
        }
    }

}
