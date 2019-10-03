/*
 * XP.RAVEN Project
 * <p/>
 * Copyright (C) 2018 Xpand IT.
 * <p/>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.Utils;

import hudson.matrix.MatrixProject;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import org.apache.commons.lang3.StringUtils;

public class BuilderUtils {

    private static final String BETWEEN_BRACES_REGEX = "^\\$\\{.*\\}$";

    /**
     * Utility method to check if the project type is supported by XrayJenkins plugin
     * @param jobType the project type
     * @return <code>true</code> if the project type is supported, <code>false</code> otherwise
     */
    public static boolean isSupportedJobType(Class<? extends AbstractProject> jobType){
        /*MatrixProject is the jobType used by Multi Configuration Project
         *MavenModuleSet is the jobType used by Maven IntegrationPlugin
         */
        return FreeStyleProject.class.isAssignableFrom(jobType)
                || MatrixProject.class.isAssignableFrom(jobType)
                || MavenModuleSet.class.isAssignableFrom(jobType);
    }

    /**
     * Utility method to check if the test execution key is invalid
     * @param testExecKey the test execution key
     * @return <code>true</code> if the test execution key matches the regex or is null/empty, <code>false</code> otherwise
     */
    public static boolean isTestExecKeyInvalid(String testExecKey) {
        if(StringUtils.isNotEmpty(testExecKey)) {
            return testExecKey.trim().matches(BETWEEN_BRACES_REGEX);
        } else return true;
    }
}
