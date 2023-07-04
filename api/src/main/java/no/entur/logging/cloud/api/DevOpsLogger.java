package no.entur.logging.cloud.api;

import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * SLF4J-style logger with more explicit error log levels for communication with
 * operations: "Tell Me Tomorrow", "Interrupt My Dinner" and "Wake Me Up RightNow".
 */

public interface DevOpsLogger extends Logger {

	void errorInterruptMyDinner(String msg);

	void errorInterruptMyDinner(String format, Object arg) ;

	void errorInterruptMyDinner(String format, Object arg1, Object arg2);

	void errorInterruptMyDinner(String format, Object... arguments);

	void errorInterruptMyDinner(String msg, Throwable t);

	boolean isInterruptMyDinnerEnabled(Marker marker);

	void errorInterruptMyDinner(Marker marker, String msg);

	void errorInterruptMyDinner(Marker marker, String format, Object arg) ;

	void errorInterruptMyDinner(Marker marker, String format, Object arg1, Object arg2);

	void errorInterruptMyDinner(Marker marker, String format, Object... arguments);

	void errorInterruptMyDinner(Marker marker, String msg, Throwable t);

	void errorWakeMeUpRightNow(String msg);

	void errorWakeMeUpRightNow(String format, Object arg);

	void errorWakeMeUpRightNow(String format, Object arg1, Object arg2);

	void errorWakeMeUpRightNow(String format, Object... arguments);

	void errorWakeMeUpRightNow(String msg, Throwable t);

	boolean isWakeMeUpRightNowEnabled(Marker marker);

	void errorWakeMeUpRightNow(Marker marker, String msg);

	void errorWakeMeUpRightNow(Marker marker, String format, Object arg);

	void errorWakeMeUpRightNow(Marker marker, String format, Object arg1, Object arg2);

	void errorWakeMeUpRightNow(Marker marker, String format, Object... arguments);

	void errorWakeMeUpRightNow(Marker marker, String msg, Throwable t);

	void errorTellMeTomorrow(String msg);

	void errorTellMeTomorrow(String format, Object arg);

	void errorTellMeTomorrow(String format, Object arg1, Object arg2);

	void errorTellMeTomorrow(String format, Object... arguments);

	void errorTellMeTomorrow(String msg, Throwable t);

	boolean isTellMeTomorrowEnabled(Marker marker);

	void errorTellMeTomorrow(Marker marker, String msg);

	void errorTellMeTomorrow(Marker marker, String format, Object arg);

	void errorTellMeTomorrow(Marker marker, String format, Object arg1, Object arg2);

	void errorTellMeTomorrow(Marker marker, String format, Object... arguments);

	void errorTellMeTomorrow(Marker marker, String msg, Throwable t);

}
