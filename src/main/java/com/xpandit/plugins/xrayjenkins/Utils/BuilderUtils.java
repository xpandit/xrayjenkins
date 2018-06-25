/*
 * XP.RAVEN Project
 * <p/>
 * Copyright (C) 2018 Xpand IT.
 * <p/>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.Utils;

import com.xpandit.plugins.xrayjenkins.model.ServerConfiguration;
import com.xpandit.plugins.xrayjenkins.model.XrayInstance;
import hudson.matrix.MatrixProject;
import hudson.maven.MavenModuleSet;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.util.ListBoxModel;
import java.util.List;

public class BuilderUtils {

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

    public static ListBoxModel doFillServerInstanceItems() {

        ListBoxModel items = new ListBoxModel();
        for(XrayInstance sc : ServerConfiguration.get().getServerInstances()){
            items.add(sc.getAlias(),sc.getConfigID());
        }
        return items;
    }

}
