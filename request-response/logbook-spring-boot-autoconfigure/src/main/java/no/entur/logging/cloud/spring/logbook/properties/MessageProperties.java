package no.entur.logging.cloud.spring.logbook.properties;

import no.entur.logging.cloud.logbook.MessageComposer;

public class MessageProperties {
    protected boolean scheme = true;
    protected boolean host = true;
    protected boolean port = true;
    protected boolean path = true;
    protected boolean query = true;

    public boolean isScheme() {
        return scheme;
    }

    public void setScheme(boolean scheme) {
        this.scheme = scheme;
    }

    public boolean isHost() {
        return host;
    }

    public void setHost(boolean host) {
        this.host = host;
    }

    public boolean isPort() {
        return port;
    }

    public void setPort(boolean port) {
        this.port = port;
    }

    public boolean isPath() {
        return path;
    }

    public void setPath(boolean path) {
        this.path = path;
    }

    public boolean isQuery() {
        return query;
    }

    public void setQuery(boolean query) {
        this.query = query;
    }

    public MessageComposer toComposer() {
        return new MessageComposer(scheme, host, port, path, query);
    }
}
