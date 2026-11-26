package no.entur.logging.cloud.logback.logstash.test.ai.full;

import no.entur.logging.cloud.logback.logstash.test.ai.common.AiMdcConverter;

/**
 * MDC for the full file (see {@link AiMdcConverter}): all entries, and the first time an alias is written it is followed by its value,
 * <code>u7(123e4567-e89b-12d3-a456-426614174000)</code>, so that values can be found in both directions.
 */
public class FullMdcConverter extends AiMdcConverter {

    public FullMdcConverter() {
        super(true);
    }
}
