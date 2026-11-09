package no.entur.logging.cloud.logback.logstash.test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.List;
import java.util.Locale;

/**
 * Leaves out events below WARN from framework loggers which only report start-up and shutdown (servers starting, servlets initialized, ...),
 * except the line announcing the port a server listens on (message contains "started" followed by "port", e.g.
 * <code>Tomcat started on port 8080</code>). Meant for the compact file; the full file keeps them. Logger names are matched by prefix on a
 * package or class boundary. Override the list with <code>-Dentur.logging.ai.quiet-loggers=a.b,c.d</code> (empty value: keep everything).
 */
public class QuietLoggersFilter extends Filter<ILoggingEvent> {


    public static final List<String> DEFAULT_QUIET_LOGGERS = List.of(
            "org.springframework.test.context.support.AnnotationConfigContextLoaderUtils",
            "org.springframework.web.servlet.DispatcherServlet",
            "org.springframework.boot.tomcat",
            "org.springframework.boot.web.context.servlet.WebApplicationContextInitializer",
            "org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver",
            "org.springframework.security.config.annotation.authentication.configuration.InitializeUserDetailsBeanManagerConfigurer",
            "org.springframework.grpc.server",
            "org.apache.catalina",
            "org.apache.coyote",
            "com.zaxxer.hikari");

    private List<String> prefixes = DEFAULT_QUIET_LOGGERS;

    @Override
    public void start() {
        prefixes = AiLoggingControl.quietLoggers(DEFAULT_QUIET_LOGGERS);
        super.start();
    }

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event.getLevel().isGreaterOrEqual(Level.WARN)) {
            return FilterReply.NEUTRAL;
        }
        String name = event.getLoggerName();
        for (String prefix : prefixes) {
            if (matches(name, prefix)) {
                String message = event.getFormattedMessage();
                return message != null && isServerStartedOnPort(message) ? FilterReply.NEUTRAL : FilterReply.DENY;
            }
        }
        return FilterReply.NEUTRAL;
    }

    static boolean matches(String name, String prefix) {
        if (!name.startsWith(prefix)) {
            return false;
        }
        if (name.length() == prefix.length()) {
            return true;
        }
        char next = name.charAt(prefix.length());
        return next == '.' || next == '$';
    }

    /** True if the message has the word 'started' followed later by the word 'port' (any case), as in 'Tomcat started on port 8080'. */
    static boolean isServerStartedOnPort(String message) {
        String lowerCase = message.toLowerCase(Locale.ROOT);
        int started = indexOfWord(lowerCase, "started", 0);
        return started != -1 && indexOfWord(lowerCase, "port", started + "started".length()) != -1;
    }

    private static int indexOfWord(String text, String word, int from) {
        int i = text.indexOf(word, from);
        while (i != -1) {
            int end = i + word.length();
            if ((i == 0 || !isWordCharacter(text.charAt(i - 1))) && (end == text.length() || !isWordCharacter(text.charAt(end)))) {
                return i;
            }
            i = text.indexOf(word, i + 1);
        }
        return -1;
    }

    private static boolean isWordCharacter(char c) {
        return c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
    }
}
