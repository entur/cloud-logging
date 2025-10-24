package no.entur.logging.cloud.appender.condition;

import ch.qos.logback.core.boolex.PropertyConditionBase;

public class AnyTrueBooleanPropertyCondition extends PropertyConditionBase {

    protected String keys;

    public void start() {
        if (keys == null) {
            addError("In AnyTrueBooleanPropertyCondition 'keys' parameter cannot be null");
            return;
        }
        super.start();
    }

    /**
     * Return the property key that will be looked up when evaluating the
     * condition.
     *
     * @return the property key, or {@code null} if not set
     */
    public String getKeys() {
        return keys;
    }

    /**
     * Set the property key to resolve during evaluation.
     *
     * @param keys the property key
     */
    public void setKeys(String keys) {
        this.keys = keys;
    }

    /**
     * Evaluate the condition: resolve the property named by {@link #keys}.
     *
     * @return {@code true} if the resolved property equals the expected
     * value; {@code false} otherwise
     */
    @Override
    public boolean evaluate() {
        if (keys == null) {
            addError("key cannot be null");
            return false;
        }

        String[] split = keys.split(",");

        for(String key : split) {
            String val = p(key);
            if (Boolean.parseBoolean(val)) { // also handles null
                return true;
            }
        }
        return false;
    }
}