/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package no.entur.logging.cloud.logback.test;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.CoreConstants;
import org.entur.jackson.tools.jsh.AnsiSyntaxHighlight;

import java.util.List;
import java.util.function.Predicate;

/**
 * Add a stack trace in case the event contains a Throwable.
 *
 * Copy of {@link ch.qos.logback.classic.pattern.ExtendedThrowableProxyConverter} + extra whitespace
 */
public class HighlightingThrowableProxyConverter extends ThrowableHandlingConverter {

    protected static final int BUILDER_CAPACITY = 2048;

    int lengthOption = Integer.MAX_VALUE;
    protected Predicate<StackTraceElement> matcher;

    @SuppressWarnings("unchecked")
    public void start() {

        List<String> optionList = getOptionList();
        if (optionList != null && !optionList.isEmpty()) {
            this.matcher = createMatcher(optionList);
        }

        super.start();
    }

    private Predicate<StackTraceElement> createMatcher(List<String> optionList) {
        if(optionList.size() == 1) {
            String option = optionList.get(0);
            return (s) -> s.getClassName().startsWith(option);
        }

        return (s) -> {
            for(String option : optionList) {
                if(s.getClassName().startsWith(option)) {
                    return true;
                }
            }

            return false;
        };

    }

    protected void extraData(StringBuilder builder, StackTraceElementProxy step) {
        // nop
    }

    public String convert(ILoggingEvent event) {
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp == null) {
            return CoreConstants.EMPTY_STRING;
        }

        return throwableProxyToString(tp);
    }

    protected String throwableProxyToString(IThrowableProxy tp) {
        StringBuilder sb = new StringBuilder(BUILDER_CAPACITY);
        sb.append(CoreConstants.LINE_SEPARATOR);

        recursiveAppend(sb, null, ThrowableProxyUtil.REGULAR_EXCEPTION_INDENT, tp);

        sb.append(CoreConstants.LINE_SEPARATOR);
        return sb.toString();
    }

    private void recursiveAppend(StringBuilder sb, String prefix, int indent, IThrowableProxy tp) {
        if (tp == null)
            return;
        subjoinFirstLine(sb, prefix, indent, tp);
        sb.append(CoreConstants.LINE_SEPARATOR);
        subjoinSTEPArray(sb, indent, tp);
        IThrowableProxy[] suppressed = tp.getSuppressed();
        if (suppressed != null) {
            for (IThrowableProxy current : suppressed) {
                recursiveAppend(sb, CoreConstants.SUPPRESSED, indent + ThrowableProxyUtil.SUPPRESSED_EXCEPTION_INDENT,
                        current);
            }
        }
        recursiveAppend(sb, CoreConstants.CAUSED_BY, indent, tp.getCause());
    }

    private void subjoinFirstLine(StringBuilder buf, String prefix, int indent, IThrowableProxy tp) {
        ThrowableProxyUtil.indent(buf, indent - 1);
        if (prefix != null) {
            buf.append(prefix);
        }
        ThrowableProxyUtil.subjoinExceptionMessage(buf, tp);
    }


    protected void subjoinSTEPArray(StringBuilder buf, int indent, IThrowableProxy tp) {
        StackTraceElementProxy[] stepArray = tp.getStackTraceElementProxyArray();
        int commonFrames = tp.getCommonFrames();

        boolean unrestrictedPrinting = lengthOption > stepArray.length;

        int maxIndex = (unrestrictedPrinting) ? stepArray.length : lengthOption;
        if (commonFrames > 0 && unrestrictedPrinting) {
            maxIndex -= commonFrames;
        }

        int ignoredCount = 0;
        for (int i = 0; i < maxIndex; i++) {
            StackTraceElementProxy element = stepArray[i];
            if (!isIgnoredStackTraceLine(element.toString())) {

                ThrowableProxyUtil.indent(buf, indent);
                if(matcher != null) {
                    if(matcher.test(element.getStackTraceElement())) {
                        buf.append(AnsiSyntaxHighlight.ESC_START);
                        buf.append(AnsiSyntaxHighlight.HIGH_INTENSITY);
                        buf.append(AnsiSyntaxHighlight.ESC_END);

                        printStackLine(buf, ignoredCount, element);

                        buf.append(AnsiSyntaxHighlight.RESET);
                    } else {
                        printStackLine(buf, ignoredCount, element);
                    }
                } else {
                    printStackLine(buf, ignoredCount, element);
                }

                ignoredCount = 0;
                buf.append(CoreConstants.LINE_SEPARATOR);
            } else {
                ++ignoredCount;
                if (maxIndex < stepArray.length) {
                    ++maxIndex;
                }
            }
        }
        if (ignoredCount > 0) {
            printIgnoredCount(buf, ignoredCount);
            buf.append(CoreConstants.LINE_SEPARATOR);
        }

        if (commonFrames > 0 && unrestrictedPrinting) {
            ThrowableProxyUtil.indent(buf, indent);
            buf.append("... ").append(tp.getCommonFrames()).append(" common frames omitted")
                    .append(CoreConstants.LINE_SEPARATOR);
        }
    }

    private void printStackLine(StringBuilder buf, int ignoredCount, StackTraceElementProxy element) {
        buf.append(element);
        extraData(buf, element); // allow other data to be added
        if (ignoredCount > 0) {
            printIgnoredCount(buf, ignoredCount);
        }
    }

    private void printIgnoredCount(StringBuilder buf, int ignoredCount) {
        buf.append(" [").append(ignoredCount).append(" skipped]");
    }

    private boolean isIgnoredStackTraceLine(String line) {
        /*
        if (ignoredStackTraceLines != null) {
            for (String ignoredStackTraceLine : ignoredStackTraceLines) {
                if (line.contains(ignoredStackTraceLine)) {
                    return true;
                }
            }
        }
         */
        return false;
    }

}
