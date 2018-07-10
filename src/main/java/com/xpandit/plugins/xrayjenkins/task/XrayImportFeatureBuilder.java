/*
 * XP.RAVEN Project
 * <p/>
 * Copyright (C) 2018 Xpand IT.
 * <p/>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.task;

import com.xpandit.plugins.xrayjenkins.Utils.BuilderUtils;
import com.xpandit.plugins.xrayjenkins.Utils.ConfigurationUtils;
import com.xpandit.plugins.xrayjenkins.Utils.FileUtils;
import com.xpandit.plugins.xrayjenkins.Utils.FormUtils;
import com.xpandit.plugins.xrayjenkins.exceptions.XrayJenkinsGenericException;
import com.xpandit.plugins.xrayjenkins.model.ServerConfiguration;
import com.xpandit.plugins.xrayjenkins.model.XrayInstance;
import com.xpandit.xray.model.FileStream;
import com.xpandit.xray.model.UploadResult;
import com.xpandit.xray.service.impl.XrayTestImporterImpl;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.annotation.Nonnull;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.entity.ContentType;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public class XrayImportFeatureBuilder extends Builder implements SimpleBuildStep{

    private String serverInstance;
    private String folderPath;
    private String projectKey;
    private String lastModified;//this must be a String because of pipeline projects

    @DataBoundConstructor
    public XrayImportFeatureBuilder(String serverInstance,
                                    String folderPath,
                                    String projectKey,
                                    String lastModified){
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

    public String getLastModified() {
        return lastModified;
    }
    @DataBoundSetter
    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run,
                        @Nonnull FilePath workspace,
                        @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws IOException, InterruptedException {
        XrayInstance xrayInstance = ConfigurationUtils.getConfiguration(this.serverInstance);
        if(xrayInstance == null){
            listener.getLogger().println("The server instance is null");
            throw new AbortException();
        }
        if (StringUtils.isBlank(this.projectKey)) {
            listener.getLogger().println("You must provide the project key");
            throw new AbortException();
        }
        if(StringUtils.isBlank(this.folderPath)){
            listener.getLogger().println("You must provide the directory path");
            throw new AbortException();
        }
        XrayTestImporterImpl client = new XrayTestImporterImpl(xrayInstance.getServerAddress(),
                xrayInstance.getUsername(),
                xrayInstance.getPassword());
        processImport(workspace, client, listener);

    }

    private void processImport(FilePath workspace,
                          XrayTestImporterImpl client,
                          TaskListener listener) throws IOException, InterruptedException {
        List<FilePath> paths;
        try{
            paths = FileUtils.getFeatureFilesFromWorkspace(workspace, this.folderPath, listener);
        } catch (IOException | InterruptedException e){
            listener.getLogger().println("An error occurred while getting the feature files");
            throw e;
        }
        for(FilePath fp : paths){
            if(isApplicableAsModifiedFile(fp)){
                FileStream f = new com.xpandit.xray.model.FileStream(fp.getName(),
                        fp.read(),
                        ContentType.APPLICATION_JSON);
                UploadResult up = client.importFeatures(this.projectKey, f);
                listener.getLogger().println(up.getMessage());
            }
        }
    }

    private boolean isApplicableAsModifiedFile(FilePath filePath) throws InterruptedException, IOException{
        if(StringUtils.isBlank(lastModified)){
            //the modified field is not used so we return true
            return true;
        }
        int lastModifiedIntValue = getLastModifiedIntValue();
        Long diffInMillis = new Date().getTime() - filePath.lastModified();
        Long diffInHour = diffInMillis /  DateUtils.MILLIS_PER_HOUR;
        return diffInHour <= lastModifiedIntValue;
    }

    private int getLastModifiedIntValue(){
        try{
            int m = Integer.parseInt(this.lastModified);
            if(m <= 0){
                throw new XrayJenkinsGenericException("last modified value must be a positive integer");
            }
            return m;
        } catch (NumberFormatException e){
            throw new XrayJenkinsGenericException("last modified value is not an integer");
        }
    }

    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Xray: Cucumber Features Import Task";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return BuilderUtils.isSupportedJobType(jobType);
        }

        public List<XrayInstance> getServerInstances(){
            return ServerConfiguration.get().getServerInstances();
        }

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

        public FormValidation doCheckLastModified(@QueryParameter String lastModified){
            if(StringUtils.isBlank(lastModified)){
                return FormValidation.ok();
            }
            try{
                return Integer.parseInt(lastModified) > 0 ? FormValidation.ok() : FormValidation.error("The value cannot be negative nor 0");
            } catch (NumberFormatException e){
                return FormValidation.error("The value must be a positive integer");
            }
        }

    }
}
