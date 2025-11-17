package no.entur.logging.cloud.logbook;

import org.zalando.logbook.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Duration;
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
            String schemeValue = request.getScheme();
            if(schemeValue != null && !schemeValue.isEmpty()) {
                messageBuilder.append(request.getScheme());
                messageBuilder.append(':');
            }
        }
        if (host) {
            String hostValue = request.getHost();
            if(hostValue != null && !hostValue.isEmpty()) {
                messageBuilder.append("//");
                messageBuilder.append(hostValue);
            }
        }
        if (port) {
            request.getPort().ifPresent(p -> {
                if (isNotStandardPort(request.getScheme(), p)) {
                    messageBuilder.append(':').append(p);
                }
            });
        }
        if (path) {
            String pathValue = request.getPath();
            if(pathValue != null && !pathValue.isEmpty()) {
                if (!pathValue.startsWith("/")) {
                    messageBuilder.append('/');
                }
                messageBuilder.append(pathValue);
            }
        }
        if (query) {
            String query = request.getQuery();
            if(query != null && !query.isEmpty()) {
                messageBuilder.append('?');
                messageBuilder.append(query);
            }
        }
    }

    public void requestMessage(HttpRequest request, StringBuilder messageBuilder) throws IOException {
        String method = request.getMethod();
        if(method != null) {
            messageBuilder.append(method);
            messageBuilder.append(' ');
        }
        constructMessage(request, messageBuilder);
    }

    public void responseMessage(Correlation correlation, HttpRequest request, HttpResponse response,
            StringBuilder messageBuilder) throws IOException {
        messageBuilder.append(response.getStatus());

        String reasonPhrase = response.getReasonPhrase();
        if (reasonPhrase != null) {
            messageBuilder.append(' ');
            messageBuilder.append(reasonPhrase);
        }
        messageBuilder.append(' ');
        constructMessage(request, messageBuilder);

        Duration duration = correlation.getDuration();
        if(duration != null) {
            messageBuilder.append(" (in ");
            messageBuilder.append(duration.toMillis());
            messageBuilder.append(" ms)");
        }
    }
}
