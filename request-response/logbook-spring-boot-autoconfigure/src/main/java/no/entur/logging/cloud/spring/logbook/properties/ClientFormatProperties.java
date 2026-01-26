package no.entur.logging.cloud.spring.logbook.properties;

public class ClientFormatProperties {

    private MessageFormatProperties message = new MessageFormatProperties();

    public MessageFormatProperties getMessage() {
        return message;
    }

    public void setMessage(MessageFormatProperties message) {
        this.message = message;
    }
}
