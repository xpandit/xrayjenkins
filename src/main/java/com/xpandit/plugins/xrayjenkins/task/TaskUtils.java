package com.xpandit.plugins.xrayjenkins.task;

import hudson.EnvVars;
import org.apache.commons.lang3.StringUtils;

class TaskUtils {
    private TaskUtils() {}

    static String expandVariable(final EnvVars environment, final String variable) {
        if(StringUtils.isNotBlank(variable)){
            final String expanded = environment.expand(variable);
            return StringUtils.equals(expanded, variable) ? variable : expanded;
        }
        return StringUtils.EMPTY;
    }
}
