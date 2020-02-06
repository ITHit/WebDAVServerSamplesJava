package com.ithit.webdav.samples.springboot.impl;

import com.ithit.webdav.server.Logger;

public class SpringBootLogger implements Logger {

    private org.slf4j.Logger logger;

    public SpringBootLogger(org.slf4j.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void logDebug(String message) {
        logger.debug(message);
    }

    @Override
    public void logError(String message, Throwable ex) {
        logger.error(message, ex);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }
}
