package no.entur.logging.cloud.spring.ondemand.web.properties;

/**
 *
 * Troubleshooting only works for requests, i.e. debugging is assumed to hold so much data that
 * caching everything until the response is ready is too costly.
 *
 */

public class OndemandTroubleshoot {

    private String level = "debug";

    private OndemandHttpRequestTrigger http = new OndemandHttpRequestTrigger();

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLevel() {
        return level;
    }

    public OndemandHttpRequestTrigger getHttp() {
        return http;
    }

    public void setHttp(OndemandHttpRequestTrigger http) {
        this.http = http;
    }
}
