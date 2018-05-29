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

public class BuilderUtils {

    /**
     * Utility method to check if the project type is supported by XrayJenkins plugin
     * @param jobType the project type
     * @return <code>true</code> if the project type is supported, <code>false</code> otherwise
     */
    public static boolean isSuportedJobType(Class<? extends AbstractProject> jobType){
        //MatrixProject is the jobType used by Multi Configuration Project
        //MavenModuleSet is the jobtype used by Maven IntegrationPlugin
        return FreeStyleProject.class.isAssignableFrom(jobType)
                || MatrixProject.class.isAssignableFrom(jobType)
                || MavenModuleSet.class.isAssignableFrom(jobType);
    }

}
