/*
 * XP.RAVEN Project
 * <p/>
 * Copyright (C) 2018 Xpand IT.
 * <p/>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.task;

import com.google.common.base.Strings;
import com.xpandit.plugins.xrayjenkins.Utils.BuilderUtils;
import com.xpandit.plugins.xrayjenkins.Utils.ConfigurationUtils;
import com.xpandit.plugins.xrayjenkins.Utils.FormUtils;
import com.xpandit.plugins.xrayjenkins.model.ServerConfiguration;
import com.xpandit.plugins.xrayjenkins.model.XrayInstance;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class XrayImportFeatureBuilder extends Builder {

    private String serverInstance;
    private String filesFolder;
    private int lastModified;

    @DataBoundConstructor
    public XrayImportFeatureBuilder(String serverInstance,
                                    String filesFolder,
                                    int lastModified){
        this.serverInstance = serverInstance;
        this.filesFolder = filesFolder;
        this.lastModified = lastModified;
    }

    public String getServerInstance() {
        return serverInstance;
    }

    public void setServerInstance(String serverInstance) {
        this.serverInstance = serverInstance;
    }

    public String getFilesFolder() {
        return filesFolder;
    }

    public void setFilesFolder(String filesFolder) {
        this.filesFolder = filesFolder;
    }

    public int getLastModified() {
        return lastModified;
    }

    public void setLastModified(int lastModified) {
        this.lastModified = lastModified;
    }

    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {


        @Override
        public String getDisplayName() {
            return "Xray: Import Features Builder";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public List<XrayInstance> getServerInstances(){
            return ServerConfiguration.get().getServerInstances();
        }

        public ListBoxModel doFillServerInstanceItems(){
            return FormUtils.getServerInstanceItems();
        }

        //TODO - validation of fields folder and server
        public FormValidation doCheckFilesFolder(@QueryParameter String filesFolder){
            return StringUtils.isNotBlank(filesFolder) ? FormValidation.ok() : FormValidation.error("You must specify the base directory.");
        }

        public FormValidation doCheckServerInstance(){
            return ConfigurationUtils.anyAvailableConfiguration() ? FormValidation.ok() : FormValidation.error("No configured Server Instances found");
        }

    }
}
