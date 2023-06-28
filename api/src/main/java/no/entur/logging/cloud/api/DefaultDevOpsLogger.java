package no.entur.logging.cloud.api;

import org.slf4j.Logger;
import org.slf4j.Marker;

public class DefaultDevOpsLogger implements DevOpsLogger {

    private final Logger delegate;

    public DefaultDevOpsLogger(Logger logger) {
        this.delegate = logger;
    }

    public String getName() {
        return delegate.getName();
    }

    public boolean isDebugEnabled() {
        return delegate.isDebugEnabled();
    }

    public void debug(String msg) {
        delegate.debug(msg);
    }

    public void debug(String format, Object arg) {
        delegate.debug(format, arg);
    }

    public void debug(String format, Object arg1, Object arg2) {
        delegate.debug(format, arg1, arg2);
    }

    public void debug(String format, Object... arguments) {
        delegate.debug(format, arguments);
    }

    public void debug(String msg, Throwable t) {
        delegate.debug(msg, t);
    }

    public boolean isDebugEnabled(Marker marker) {
        return delegate.isDebugEnabled();
    }

    public void debug(Marker marker, String msg) {
        delegate.debug(marker, msg);
    }

    public void debug(Marker marker, String format, Object arg) {
        delegate.debug(format, format, arg);
    }

    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        delegate.debug(marker, format, arg1, arg2);
    }

    public void debug(Marker marker, String format, Object... arguments) {
        delegate.debug(marker, format, arguments);
    }

    public void debug(Marker marker, String msg, Throwable t) {
        delegate.debug(marker, msg, t);
    }

    public boolean isInfoEnabled() {
        return delegate.isInfoEnabled();
    }

    public void info(String msg) {
        delegate.info(msg);
    }

    public void info(String format, Object arg) {
        delegate.info(format, arg);
    }

    public void info(String format, Object arg1, Object arg2) {
        delegate.info(format, arg1, arg2);
    }

    public void info(String format, Object... arguments) {
        delegate.info(format, arguments);
    }

    public void info(String msg, Throwable t) {
        delegate.info(msg, t);
    }

    public boolean isInfoEnabled(Marker marker) {
        return delegate.isInfoEnabled(marker);
    }

    public void info(Marker marker, String msg) {
        delegate.info(marker, msg);
    }

    public void info(Marker marker, String format, Object arg) {
        delegate.info(marker, format, arg);
    }

    public void info(Marker marker, String format, Object arg1, Object arg2) {
        delegate.info(marker, format, arg1, arg2);
    }

    public void info(Marker marker, String format, Object... arguments) {
        delegate.info(marker, format, arguments);
    }

    public void info(Marker marker, String msg, Throwable t) {
        delegate.info(marker, msg, t);
    }

    public boolean isWarnEnabled() {
        return delegate.isWarnEnabled();
    }

    public void warn(String msg) {
        delegate.warn(msg);

    }

    public void warn(String format, Object arg) {
        delegate.warn(format, arg);
    }

    public void warn(String format, Object... arguments) {
        delegate.warn(format, arguments);

    }

    public void warn(String format, Object arg1, Object arg2) {
        delegate.warn(format, arg1, arg2);

    }

    public void warn(String msg, Throwable t) {
        delegate.warn(msg, t);
    }

    public boolean isWarnEnabled(Marker marker) {
        return delegate.isWarnEnabled();
    }

    public void warn(Marker marker, String msg) {
        delegate.warn(marker, msg);
    }

    public void warn(Marker marker, String format, Object arg) {
        delegate.warn(marker, format, arg);
    }

    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        delegate.warn(marker, format, arg1, arg2);
    }

    public void warn(Marker marker, String format, Object... arguments) {
        delegate.warn(marker, format, arguments);
    }

    public void warn(Marker marker, String msg, Throwable t) {
        delegate.warn(marker, msg, t);
    }

    public boolean isErrorEnabled() {
        return delegate.isErrorEnabled();
    }

    public void errorInterruptMyDinner(String msg) {
        delegate.error(DevOpsMarker.errorInterruptMyDinner(), msg);
    }

    public void errorInterruptMyDinner(String format, Object arg) {
        delegate.error(DevOpsMarker.errorInterruptMyDinner(), format, arg);
    }

    public void errorInterruptMyDinner(String format, Object arg1, Object arg2) {
        delegate.error(DevOpsMarker.errorInterruptMyDinner(), format, arg1, arg2);
    }

    public void errorInterruptMyDinner(String format, Object... arguments) {
        delegate.error(DevOpsMarker.errorInterruptMyDinner(), format, arguments);
    }

    public void errorInterruptMyDinner(String msg, Throwable t) {
        delegate.error(DevOpsMarker.errorInterruptMyDinner(), msg, t);
    }

    public boolean isInterruptMyDinnerEnabled(Marker marker) {
        return delegate.isErrorEnabled(DevOpsMarker.errorInterruptMyDinner(marker));
    }

    public void errorInterruptMyDinner(Marker marker, String msg) {
        delegate.error(DevOpsMarker.errorInterruptMyDinner(marker), msg);
    }

    public void errorInterruptMyDinner(Marker marker, String format, Object arg) {
        delegate.error(DevOpsMarker.errorInterruptMyDinner(marker), format, arg);
    }

    public void errorInterruptMyDinner(Marker marker, String format, Object arg1, Object arg2) {
        delegate.error(DevOpsMarker.errorInterruptMyDinner(marker), format, arg1, arg2);
    }

    public void errorInterruptMyDinner(Marker marker, String format, Object... arguments) {
        delegate.error(DevOpsMarker.errorInterruptMyDinner(marker), format, arguments);
    }

    public void errorInterruptMyDinner(Marker marker, String msg, Throwable t) {
        delegate.error(DevOpsMarker.errorInterruptMyDinner(marker), msg, t);
    }

    public void errorWakeMeUpRightNow(String msg) {
        delegate.error(DevOpsMarker.errorWakeMeUpRightNow(), msg);
    }

    public void errorWakeMeUpRightNow(String format, Object arg) {
        delegate.error(DevOpsMarker.errorWakeMeUpRightNow(), format, arg);
    }

    public void errorWakeMeUpRightNow(String format, Object arg1, Object arg2) {
        delegate.error(DevOpsMarker.errorWakeMeUpRightNow(), format, arg1, arg2);
    }

    public void errorWakeMeUpRightNow(String format, Object... arguments) {
        delegate.error(DevOpsMarker.errorWakeMeUpRightNow(), format, arguments);
    }

    public void errorWakeMeUpRightNow(String msg, Throwable t) {
        delegate.error(DevOpsMarker.errorWakeMeUpRightNow(), msg, t);
    }

    public boolean isWakeMeUpRightNowEnabled(Marker marker) {
        return delegate.isErrorEnabled(DevOpsMarker.errorWakeMeUpRightNow(marker));
    }

    public void errorWakeMeUpRightNow(Marker marker, String msg) {
        delegate.error(DevOpsMarker.errorWakeMeUpRightNow(marker), msg);
    }

    public void errorWakeMeUpRightNow(Marker marker, String format, Object arg) {
        delegate.error(DevOpsMarker.errorWakeMeUpRightNow(marker), format, arg);
    }

    public void errorWakeMeUpRightNow(Marker marker, String format, Object arg1, Object arg2) {
        delegate.error(DevOpsMarker.errorWakeMeUpRightNow(marker), format, arg1, arg2);
    }

    public void errorWakeMeUpRightNow(Marker marker, String format, Object... arguments) {
        delegate.error(DevOpsMarker.errorWakeMeUpRightNow(marker), format, arguments);
    }

    public void errorWakeMeUpRightNow(Marker marker, String msg, Throwable t) {
        delegate.error(DevOpsMarker.errorWakeMeUpRightNow(marker), msg, t);
    }

    public void errorTellMeTomorrow(String msg) {
        delegate.error(DevOpsMarker.errorTellMeTomorrow(), msg);
    }

    public void errorTellMeTomorrow(String format, Object arg) {
        delegate.error(DevOpsMarker.errorTellMeTomorrow(), format, arg);
    }

    public void errorTellMeTomorrow(String format, Object arg1, Object arg2) {
        delegate.error(DevOpsMarker.errorTellMeTomorrow(), format, arg1, arg2);
    }

    public void errorTellMeTomorrow(String format, Object... arguments) {
        delegate.error(DevOpsMarker.errorTellMeTomorrow(), format, arguments);
    }

    public void errorTellMeTomorrow(String msg, Throwable t) {
        delegate.error(DevOpsMarker.errorTellMeTomorrow(), msg, t);
    }

    public boolean isTellMeTomorrowEnabled(Marker marker) {
        return delegate.isErrorEnabled(DevOpsMarker.errorTellMeTomorrow(marker));
    }

    public void errorTellMeTomorrow(Marker marker, String msg) {
        delegate.error(DevOpsMarker.errorTellMeTomorrow(marker), msg);
    }

    public void errorTellMeTomorrow(Marker marker, String format, Object arg) {
        delegate.error(DevOpsMarker.errorTellMeTomorrow(marker), format, arg);
    }

    public void errorTellMeTomorrow(Marker marker, String format, Object arg1, Object arg2) {
        delegate.error(DevOpsMarker.errorTellMeTomorrow(marker), format, arg1, arg2);
    }

    public void errorTellMeTomorrow(Marker marker, String format, Object... arguments) {
        delegate.error(DevOpsMarker.errorTellMeTomorrow(marker), format, arguments);
    }

    public void errorTellMeTomorrow(Marker marker, String msg, Throwable t) {
        delegate.error(DevOpsMarker.errorTellMeTomorrow(marker), msg, t);
    }

    // deprecated methods
    /**
     * Log a message at the ERROR level.
     *
     * @param msg the message string to be logged
     * @deprecated in favor of {@linkplain #errorTellMeTomorrow(String)}
     */
    @Deprecated
    public void error(String msg) {
        delegate.error(msg);
    }

    /**
     * @param format the format string
     * @param arg    the argument
     * @deprecated in favor of {@linkplain #errorTellMeTomorrow(String)}
     */

    @Deprecated
    public void error(String format, Object arg) {
        delegate.error(format, arg);
    }

    /**
     *
     * @param format the format string
     * @param arg1   the first argument
     * @param arg2   the second argument
     * @deprecated in favor of {@linkplain #errorTellMeTomorrow(String)}
     */
    @Deprecated
    public void error(String format, Object arg1, Object arg2) {
        delegate.error(format, arg1, arg2);
    }

    /**
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     * @deprecated in favor of {@linkplain #errorTellMeTomorrow(String)}
     */
    @Deprecated
    public void error(String format, Object... arguments) {
        delegate.error(format, arguments);
    }

    /**
     * @param msg the message accompanying the exception
     * @param t   the exception (throwable) to log
     * @deprecated in favor of {@linkplain #errorTellMeTomorrow(String)}
     */

    @Deprecated
    public void error(String msg, Throwable t) {
        delegate.error(msg, t);
    }

    /**
     * @param marker The marker data to take into consideration
     * @return True if this Logger is enabled for the ERROR level,
     *         false otherwise.
     * @deprecated in favor of {@linkplain #errorTellMeTomorrow(String)}
     */

    @Deprecated
    public boolean isErrorEnabled(Marker marker) {
        return delegate.isErrorEnabled(marker);
    }

    /**
     *
     * @param marker The marker specific to this log statement
     * @param msg    the message string to be logged
     * @deprecated in favor of {@linkplain #errorTellMeTomorrow(String)}
     */

    @Deprecated
    public void error(Marker marker, String msg) {
        delegate.error(marker, msg);
    }

    /**
     *
     * @param marker the marker data specific to this log statement
     * @param format the format string
     * @param arg    the argument
     * @deprecated in favor of {@linkplain #errorTellMeTomorrow(String)}
     */

    @Deprecated
    public void error(Marker marker, String format, Object arg) {
        delegate.error(marker, format, arg);
    }

    /**
     *
     * @param marker the marker data specific to this log statement
     * @param format the format string
     * @param arg1 the first argument
     * @param arg2 the second argument
     * @deprecated in favor of {@linkplain #errorTellMeTomorrow(String)}
     */

    @Deprecated
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        delegate.error(marker, format, arg1, arg2);
    }

    /**
     *
     * @param marker    the marker data specific to this log statement
     * @param format    the format string
     * @param arguments a list of 3 or more arguments
     * @deprecated in favor of {@linkplain #errorTellMeTomorrow(String)}
     */

    @Deprecated
    public void error(Marker marker, String format, Object... arguments) {
        delegate.error(marker, format, arguments);
    }

    /**
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message accompanying the exception
     * @param t      the exception (throwable) to log
     * @deprecated in favor of {@linkplain #errorTellMeTomorrow(String)}
     */

    @Deprecated
    public void error(Marker marker, String msg, Throwable t) {
        delegate.error(marker, msg, t);
    }

    @Override
    public boolean isTraceEnabled() {
        return delegate.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        delegate.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        delegate.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        delegate.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        delegate.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        delegate.trace(msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return delegate.isTraceEnabled(marker);
    }

    @Override
    public void trace(Marker marker, String msg) {
        delegate.trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        delegate.trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        delegate.trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        delegate.trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        delegate.trace(marker, msg, t);
    }
}
