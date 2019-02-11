package com.sdl.dxa.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TimerLogger.
 */
public class TimerLogger {

    private static final Logger LOG = LoggerFactory.getLogger(TimerLogger.class);

    private TimerLogger() {}

    public static void log(String what, long duration) {
        LOG.info("{} took: {} ms.", what, duration);
    }
}
