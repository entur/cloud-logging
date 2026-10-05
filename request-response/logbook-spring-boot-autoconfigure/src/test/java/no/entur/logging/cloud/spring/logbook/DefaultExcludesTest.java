package no.entur.logging.cloud.spring.logbook;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.zalando.logbook.Correlation;
import org.zalando.logbook.HttpRequest;
import org.zalando.logbook.HttpResponse;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.Origin;
import org.zalando.logbook.Precorrelation;
import org.zalando.logbook.Sink;
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration;
import org.zalando.logbook.test.MockHttpRequest;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.google.common.truth.Truth.assertThat;

/**
 * The default (actuator, openapi) excludes apply to incoming (server) requests only, not to outgoing (client) requests.
 */

public class DefaultExcludesTest {

    private final CapturingSink sink = new CapturingSink();

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LogbookLoggingAutoConfiguration.class, LogbookAutoConfiguration.class))
            // provided by the platform (GCP or Azure) modules
            .withBean(LogbookLoggingCloudProperties.class, LogbookLoggingCloudProperties::new)
            .withBean(Sink.class, () -> sink);

    @Test
    public void excludesIncomingRequestsOnly() {
        contextRunner.run(context -> {
            Logbook logbook = context.getBean(Logbook.class);

            assertThat(isLogged(logbook, Origin.REMOTE, "/actuator/health")).isFalse();
            assertThat(isLogged(logbook, Origin.REMOTE, "/v3/api-docs/swagger-config")).isFalse();
            assertThat(isLogged(logbook, Origin.REMOTE, "/api/entity")).isTrue();

            // i.e. calling another service's actuator endpoint
            assertThat(isLogged(logbook, Origin.LOCAL, "/actuator/health")).isTrue();
            assertThat(isLogged(logbook, Origin.LOCAL, "/v3/api-docs/swagger-config")).isTrue();
        });
    }

    @Test
    public void defaultExcludesCanBeDisabled() {
        contextRunner
                .withPropertyValues("entur.logging.request-response.logbook.default-excludes=false")
                .run(context -> {
                    Logbook logbook = context.getBean(Logbook.class);

                    assertThat(isLogged(logbook, Origin.REMOTE, "/actuator/health")).isTrue();
                });
    }

    @Test
    public void keepsConfiguredExcludes() {
        contextRunner
                .withPropertyValues(
                        "logbook.predicate.exclude[0].path=/both",
                        "logbook.server.predicate.exclude[0].path=/incoming",
                        "logbook.server.predicate.exclude[1].path=/internal/**",
                        "logbook.server.predicate.exclude[1].methods=GET",
                        "logbook.client.predicate.exclude[0].path=/outgoing")
                .run(context -> {
                    Logbook logbook = context.getBean(Logbook.class);

                    assertThat(isLogged(logbook, Origin.REMOTE, "/both")).isFalse();
                    assertThat(isLogged(logbook, Origin.LOCAL, "/both")).isFalse();

                    assertThat(isLogged(logbook, Origin.REMOTE, "/incoming")).isFalse();
                    assertThat(isLogged(logbook, Origin.LOCAL, "/incoming")).isTrue();

                    assertThat(isLogged(logbook, Origin.REMOTE, "GET", "/internal/status")).isFalse();
                    assertThat(isLogged(logbook, Origin.REMOTE, "POST", "/internal/status")).isTrue();

                    assertThat(isLogged(logbook, Origin.REMOTE, "/outgoing")).isTrue();
                    assertThat(isLogged(logbook, Origin.LOCAL, "/outgoing")).isFalse();

                    // defaults are still added
                    assertThat(isLogged(logbook, Origin.REMOTE, "/actuator/health")).isFalse();
                });
    }

    private boolean isLogged(Logbook logbook, Origin origin, String path) throws IOException {
        return isLogged(logbook, origin, "GET", path);
    }

    private boolean isLogged(Logbook logbook, Origin origin, String method, String path) throws IOException {
        sink.requests.clear();
        logbook.process(MockHttpRequest.create().withOrigin(origin).withMethod(method).withPath(path)).write();
        return !sink.requests.isEmpty();
    }

    private static class CapturingSink implements Sink {

        private final List<HttpRequest> requests = new CopyOnWriteArrayList<>();

        @Override
        public void write(Precorrelation precorrelation, HttpRequest request) {
            requests.add(request);
        }

        @Override
        public void write(Correlation correlation, HttpRequest request, HttpResponse response) {
        }
    }
}
