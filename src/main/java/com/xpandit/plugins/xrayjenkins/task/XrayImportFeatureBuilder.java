/*
 * XP.RAVEN Project
 * <p/>
 * Copyright (C) 2018 Xpand IT.
 * <p/>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.task;

import com.xpandit.plugins.xrayjenkins.Utils.ConfigurationUtils;
import com.xpandit.plugins.xrayjenkins.Utils.FormUtils;
import com.xpandit.plugins.xrayjenkins.model.ServerConfiguration;
import com.xpandit.plugins.xrayjenkins.model.XrayInstance;
import com.xpandit.xray.model.UploadResult;
import com.xpandit.xray.service.impl.XrayTestImporterImpl;
import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class XrayImportFeatureBuilder extends Builder {

    private String serverInstance;
    private String folderPath;
    private String projectKey;
    private int lastModified;

    @DataBoundConstructor
    public XrayImportFeatureBuilder(String serverInstance,
                                    String folderPath,
                                    String projectKey,
                                    int lastModified){
        this.serverInstance = serverInstance;
        this.folderPath = folderPath;
        this.projectKey = projectKey;
        this.lastModified = lastModified;
    }

    public String getServerInstance() {
        return serverInstance;
    }

    public void setServerInstance(String serverInstance) {
        this.serverInstance = serverInstance;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public int getLastModified() {
        return lastModified;
    }

    public void setLastModified(int lastModified) {
        this.lastModified = lastModified;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build,
                           Launcher launcher,
                           BuildListener listener) throws InterruptedException, IOException {
        XrayInstance xrayInstance = ConfigurationUtils.getConfiguration(this.serverInstance);
        if(xrayInstance == null){
            listener.getLogger().println("The server instance is null");
            throw new AbortException();
        }
        File folderPath = new File(this.folderPath);
        if(!folderPath.isDirectory()){
            listener.getLogger().println("The location is not a directory");
            throw new AbortException();
        }
        XrayTestImporterImpl client = new XrayTestImporterImpl(xrayInstance.getServerAddress(),
                                                                xrayInstance.getUsername(),
                                                                xrayInstance.getPassword());
        processFolderImport(client, listener, folderPath);
        return true;
    }

    public void processFolderImport(XrayTestImporterImpl client, BuildListener listener, File folderPath){
        File [] folderFiles = folderPath.listFiles();
        for(File f : folderFiles){
            if(f.isDirectory()){
                processFolderImport(client, listener, f);
            } else{ //TODO - must i check f.exists()?
                UploadResult result = client.importFeatures(this.projectKey, f);
                listener.getLogger().println(result.getMessage());
            }
        }
    }

    //TODO - check if timezones are not screwing this algorithm
    private boolean isApplicableAsModifiedFile(File f){
        if(this.lastModified == 0){
            return true;
        }
        Long diffInMillis = new Date().getTime() - f.lastModified();
        Long diffInHour = ((diffInMillis / 100) / 60) / 60;
        return diffInHour <= lastModified ? true : false;
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


        //todo - the client will still be able to click save so we'll need to check for null or valid values when perform
        public ListBoxModel doFillServerInstanceItems(){
            return FormUtils.getServerInstanceItems();
        }

        public FormValidation doCheckFolderPath(@QueryParameter String folderPath){
            return StringUtils.isNotBlank(folderPath) ? FormValidation.ok() : FormValidation.error("You must specify the base directory.");
        }

        public FormValidation doCheckServerInstance(){
            return ConfigurationUtils.anyAvailableConfiguration() ? FormValidation.ok() : FormValidation.error("No configured Server Instances found");
        }

        public FormValidation doCheckProjectKey(@QueryParameter String projectKey){
            return StringUtils.isNotBlank(projectKey) ? FormValidation.ok() : FormValidation.error("You must specify the Project key");
        }

    }
}
