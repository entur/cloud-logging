package no.entur.logging.cloud.spring.logbook;

public class LogbookLoggingCloudProperties {

    protected int maxSize;

    protected int maxBodySize;

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getMaxBodySize() {
        return maxBodySize;
    }

    public void setMaxBodySize(int maxBodySize) {
        this.maxBodySize = maxBodySize;
    }

}
