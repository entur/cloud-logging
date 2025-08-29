package no.entur.logging.cloud.logbook;

import org.zalando.logbook.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 *
 * Request-response log message composer.
 *
 */

public class MessageComposer {

    protected boolean scheme;
    protected boolean host;
    protected boolean port;
    protected boolean path;
    protected boolean query;

    public MessageComposer(boolean scheme, boolean host, boolean port, boolean path, boolean query) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.path = path;
        this.query = query;
    }

    static boolean isNotStandardPort(final String scheme, final int port) {
        return ("http".equals(scheme) && port != 80) ||
                ("https".equals(scheme) && port != 443);
    }

    protected void constructMessage(HttpRequest request, StringBuilder messageBuilder) throws IOException {
        if (scheme) {
            messageBuilder.append(request.getScheme());
            messageBuilder.append(':');
        }
        if (host) {
            messageBuilder.append("//");
            messageBuilder.append(request.getHost());
        }
        if (port) {
            request.getPort().ifPresent(p -> {
                if (isNotStandardPort(request.getScheme(), p)) {
                    messageBuilder.append(':').append(p);
                }
            });
        }
        if (path) {
            if (!request.getPath().startsWith("/")) {
                messageBuilder.append('/');
            }
            messageBuilder.append(request.getPath());
        }
        if (query && request.getQuery() != null && !request.getQuery().isEmpty()) {
            messageBuilder.append('?');
            messageBuilder.append(request.getQuery());
        }
    }

    public void requestMessage(HttpRequest request, StringBuilder messageBuilder) throws IOException {
        messageBuilder.append(request.getMethod());
        messageBuilder.append(' ');
        constructMessage(request, messageBuilder);
    }

    public void responseMessage(Correlation correlation, HttpRequest request, HttpResponse response,
            StringBuilder messageBuilder) throws IOException {
        messageBuilder.append(response.getStatus());
        final String reasonPhrase = response.getReasonPhrase();
        if (reasonPhrase != null) {
            messageBuilder.append(' ');
            messageBuilder.append(reasonPhrase);
        }
        messageBuilder.append(' ');
        constructMessage(request, messageBuilder);

        messageBuilder.append(" (in ");
        messageBuilder.append(correlation.getDuration().toMillis());
        messageBuilder.append(" ms)");
    }
}
