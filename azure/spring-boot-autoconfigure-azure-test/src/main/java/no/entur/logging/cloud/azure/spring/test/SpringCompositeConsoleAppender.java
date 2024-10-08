package no.entur.logging.cloud.azure.spring.test;

import no.entur.logging.cloud.logback.logstash.test.CompositeConsoleAppender;
import org.springframework.boot.ansi.AnsiOutput;

public class SpringCompositeConsoleAppender extends CompositeConsoleAppender {

    // set the colour output here
    // this is difficult to do via other mechanisms like spring boot starter
    static {
        AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
    }

}
