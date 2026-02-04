package no.entur.logging.cloud.logback.test;

import ch.qos.logback.classic.pattern.ExtendedThrowableProxyConverter;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.CoreConstants;

/**
 * {@link ExtendedThrowableProxyConverter} that adds some additional whitespace around the
 * stack trace.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class ExtendedWhitespaceThrowableProxyConverter extends ExtendedThrowableProxyConverter {

    private String keys;

    public ExtendedWhitespaceThrowableProxyConverter() {
        super();
    }

    @Override
    public void start() {
        String datePattern = this.getFirstOption();
        System.out.println("Detect " + datePattern);
        super.start();
    }

    @Override
	protected String throwableProxyToString(IThrowableProxy tp) {
		String str = CoreConstants.LINE_SEPARATOR + super.throwableProxyToString(tp) + CoreConstants.LINE_SEPARATOR;

        try {
            StringBuilder builder = new StringBuilder();

            String[] parts = str.split("\n");
            for (String part : parts) {
                boolean format = part.contains("no.entur") || part.contains("org.entur");
                if (format) {
                    builder.append("-> ");
                }
                builder.append(part);
                builder.append("\n");
            }

            return builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
	}

    public void setKeys(String keys) {
        this.keys = keys;
    }

    public String getKeys() {
        return keys;
    }
}
