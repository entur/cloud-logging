
package no.entur.logging.cloud.gcp.logback.logstash;

import ch.qos.logback.classic.Level;
import no.entur.logging.cloud.api.DevOpsLevel;

public enum StackdriverSeverity  {
	/**
	 * <pre>
	 * (0) The log entry has no assigned severity level.
	 * </pre>
	 *
	 * <code>DEFAULT = 0;</code>
	 */
	DEFAULT(0),
	/**
	 * <pre>
	 * (100) Debug or trace information.
	 * </pre>
	 *
	 * <code>DEBUG = 100;</code>
	 */
	DEBUG(100),
	/**
	 * <pre>
	 * (200) Routine information, such as ongoing status or performance.
	 * </pre>
	 *
	 * <code>INFO = 200;</code>
	 */
	INFO(200),
	/**
	 * <pre>
	 * (300) Normal but significant events, such as start up, shut down, or
	 * a configuration change.
	 * </pre>
	 *
	 * <code>NOTICE = 300;</code>
	 */
	NOTICE(300),
	/**
	 * <pre>
	 * (400) Warning events might cause problems.
	 * </pre>
	 *
	 * <code>WARNING = 400;</code>
	 */
	WARNING(400),
	/**
	 * <pre>
	 * (500) Error events are likely to cause problems.
	 * </pre>
	 *
	 * <code>ERROR = 500;</code>
	 */
	ERROR(500),
	/**
	 * <pre>
	 * (600) Critical events cause more severe problems or outages.
	 * </pre>
	 *
	 * <code>CRITICAL = 600;</code>
	 */
	CRITICAL(600),
	/**
	 * <pre>
	 * (700) A person must take an action immediately.
	 * </pre>
	 *
	 * <code>ALERT = 700;</code>
	 */
	ALERT(700),
	/**
	 * <pre>
	 * (800) One or more systems are unusable.
	 * </pre>
	 *
	 * <code>EMERGENCY = 800;</code>
	 */
	EMERGENCY(800),
	UNRECOGNIZED(-1),
	;

	/**
	 * <pre>
	 * (0) The log entry has no assigned severity level.
	 * </pre>
	 *
	 * <code>DEFAULT = 0;</code>
	 */
	public static final int DEFAULT_VALUE = 0;
	/**
	 * <pre>
	 * (100) Debug or trace information.
	 * </pre>
	 *
	 * <code>DEBUG = 100;</code>
	 */
	public static final int DEBUG_VALUE = 100;
	/**
	 * <pre>
	 * (200) Routine information, such as ongoing status or performance.
	 * </pre>
	 *
	 * <code>INFO = 200;</code>
	 */
	public static final int INFO_VALUE = 200;
	/**
	 * <pre>
	 * (300) Normal but significant events, such as start up, shut down, or
	 * a configuration change.
	 * </pre>
	 *
	 * <code>NOTICE = 300;</code>
	 */
	public static final int NOTICE_VALUE = 300;
	/**
	 * <pre>
	 * (400) Warning events might cause problems.
	 * </pre>
	 *
	 * <code>WARNING = 400;</code>
	 */
	public static final int WARNING_VALUE = 400;
	/**
	 * <pre>
	 * (500) Error events are likely to cause problems.
	 * </pre>
	 *
	 * <code>ERROR = 500;</code>
	 */
	public static final int ERROR_VALUE = 500;
	/**
	 * <pre>
	 * (600) Critical events cause more severe problems or outages.
	 * </pre>
	 *
	 * <code>CRITICAL = 600;</code>
	 */
	public static final int CRITICAL_VALUE = 600;
	/**
	 * <pre>
	 * (700) A person must take an action immediately.
	 * </pre>
	 *
	 * <code>ALERT = 700;</code>
	 */
	public static final int ALERT_VALUE = 700;
	/**
	 * <pre>
	 * (800) One or more systems are unusable.
	 * </pre>
	 *
	 * <code>EMERGENCY = 800;</code>
	 */
	public static final int EMERGENCY_VALUE = 800;


	public final int getNumber() {
		if (this == UNRECOGNIZED) {
			throw new IllegalArgumentException(
					"Can't get the number of an unknown enum value.");
		}
		return value;
	}

	public static StackdriverSeverity forNumber(int value) {
		switch (value) {
		case 0: return DEFAULT;
		case 100: return DEBUG;
		case 200: return INFO;
		case 300: return NOTICE;
		case 400: return WARNING;
		case 500: return ERROR;
		case 600: return CRITICAL;
		case 700: return ALERT;
		case 800: return EMERGENCY;
		default: return null;
		}
	}

	protected static Level toLogbackLevel(StackdriverSeverity severity) {

		switch(severity) {
		case DEFAULT : {
			return Level.TRACE;
		}
		case DEBUG: {
			return Level.DEBUG;
		}
		case INFO: 
		case NOTICE: {
			return Level.INFO;
		}
		case WARNING: {
			return Level.WARN;
		}
		case ERROR :
		case CRITICAL :
		case ALERT :
		case EMERGENCY : {
			return Level.ERROR;
		}
		default : {
			throw new RuntimeException("Unexpected severity " + severity);
		}

		}
	}

	public static StackdriverSeverity forDevOpsLevel(DevOpsLevel severity) {

		switch(severity) {
			case TRACE: return StackdriverSeverity.DEFAULT;
			case DEBUG: return StackdriverSeverity.DEBUG;
			case INFO: return StackdriverSeverity.INFO;
			case WARN: return StackdriverSeverity.WARNING;
			case ERROR: return StackdriverSeverity.ERROR;
			case ERROR_TELL_ME_TOMORROW: return StackdriverSeverity.ERROR;
			case ERROR_INTERRUPT_MY_DINNER: return StackdriverSeverity.CRITICAL;
			case ERROR_WAKE_ME_UP_RIGHT_NOW: return StackdriverSeverity.ALERT;
			default : {
				throw new RuntimeException("Unexpected severity " + severity);
			}

		}
	}

	public DevOpsLevel toDevOpsLevel() {

		switch(this) {
			case DEFAULT: return DevOpsLevel.TRACE;
			case DEBUG: return DevOpsLevel.DEBUG;
			case INFO: return DevOpsLevel.INFO;
			case WARNING: return DevOpsLevel.WARN;
			case ERROR: return DevOpsLevel.ERROR_TELL_ME_TOMORROW;
			case CRITICAL: return DevOpsLevel.ERROR_INTERRUPT_MY_DINNER;
			case ALERT: return DevOpsLevel.ERROR_WAKE_ME_UP_RIGHT_NOW;
			default : {
				throw new RuntimeException("Unexpected severity " + this);
			}

		}
	}


	private final int value;

	private StackdriverSeverity(int value) {
		this.value = value;
	}

	public Level toLogbackLevel() {
		return toLogbackLevel(this);
	}
	
    /**
     * Returns <code>true</code> if this Level has a higher or equal severity than
     * the severity passed as argument, <code>false</code> otherwise.
     * 
     * @param r severity
     * @return true if greater
     */
	
    public boolean isGreaterOrEqual(StackdriverSeverity r) {
        return value >= r.value;
    }

}

