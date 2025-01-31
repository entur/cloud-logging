package no.entur.logging.cloud.spring.ondemand.web.properties;

/**
 *
 * Configure when to flush log statements; some log statements should always be written, so technically do not need to
 * be cached. But in case there is other log statements which end up being written, log statements appear out of order
 * even within a single request.<br><br>
 *
 * Log accumulation tools will normally sort the log statements on time, so order does not matter. But for local
 * development (i.e. printing to console) this is not the same.
 *
 */

public enum OndemandFlushMode {

    /**
     *
     * Flush log statements as soon it is determined they should be written.
     * Results in out-of-order log statements. Uses less memory.
     *
     */
    EAGER("eager"),

    /**
     *
     * Flush when the on-demand scope closes, i.e. after all log statements have been made.
     * Results in-order log statements. Uses more memory.
     *
     */
    LAZY("lazy");

    private final String id;

    OndemandFlushMode(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
