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
import com.xpandit.xray.service.impl.XrayTestImporterImpl;
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
        XrayTestImporterImpl client = new XrayTestImporterImpl(xrayInstance.getServerAddress(),xrayInstance.getUsername(),xrayInstance.getPassword());
        File folderPath = new File(this.folderPath);
        File [] folderFiles = folderPath.listFiles();
        for(File f : folderFiles){
            client.importFeatures(this.projectKey, f);
        }
        return true;
    }

    /*//TODO - delete this
    private FilePath getFile(FilePath workspace, String filePath, TaskListener listener) throws IOException, InterruptedException{
        if(workspace == null){
            throw new XrayJenkinsGenericException("No workspace in this current node");
        }

        if(StringUtils.isBlank(filePath)){
            throw new XrayJenkinsGenericException("No file path was specified");
        }

        FilePath file = readFile(workspace,filePath.trim(),listener);

        if(file.isDirectory() || !file.exists()){
            throw new XrayJenkinsGenericException("File path is a directory or the file doesn't exist");
        }
        return file;
    }
    //TODO - delete this
    private FilePath readFile(FilePath workspace, String filePath, TaskListener listener) throws IOException{
        FilePath f = new FilePath(workspace, filePath);
        listener.getLogger().println("File: "+f.getRemote());
        return f;
    }*/

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
