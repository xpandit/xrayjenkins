package com.xpandit.plugins.xrayjenkins.task;

import hudson.EnvVars;
import org.apache.commons.lang3.StringUtils;

class TaskUtils {
    private TaskUtils() {}

    /**
     * Tries to expand a variable value, using the Jenkins Variable Environment.
     * Example: ${ISSUEKEY} will be replaced by the value defined in the Environment, if it's defined.
     * 
     * @param environment Jenkins Variable environment.
     * @param variable the variable to be replaced
     * @return the variable value, if it's defined, otherwise, it will return the variable itself.
     */
    static String expandVariable(final EnvVars environment, final String variable) {
        if (environment == null) {
            return StringUtils.defaultString(variable);
        } else if (StringUtils.isNotBlank(variable)) {
            final String expanded = environment.expand(variable);
            return StringUtils.equals(expanded, variable) ? variable : expanded;
        }
        return StringUtils.EMPTY;
    }
}
