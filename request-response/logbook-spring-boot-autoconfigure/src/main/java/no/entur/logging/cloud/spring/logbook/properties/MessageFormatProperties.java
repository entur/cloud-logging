package no.entur.logging.cloud.spring.logbook.properties;

import no.entur.logging.cloud.logbook.MessageComposer;

public class MessageFormatProperties {

    /** Custom prefix */
    protected String prefix = null;
    /** Include URI protocol / scheme */
    protected boolean scheme = true;
    /** Include URI host */
    protected boolean host = true;
    /** Include URI port */
    protected boolean port = true;
    /** Include URI path */
    protected boolean path = true;
    /** Include URI query */
    protected boolean query = true;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

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
        return new MessageComposer(prefix, scheme, host, port, path, query);
    }
}
