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
import com.xpandit.plugins.xrayjenkins.Utils.ProxyUtil;
import com.xpandit.plugins.xrayjenkins.exceptions.XrayJenkinsGenericException;
import com.xpandit.plugins.xrayjenkins.model.HostingType;
import com.xpandit.plugins.xrayjenkins.model.ServerConfiguration;
import com.xpandit.plugins.xrayjenkins.model.XrayInstance;
import com.xpandit.plugins.xrayjenkins.task.filefilters.OnlyFeatureFilesInPathFilter;
import com.xpandit.xray.exception.XrayClientCoreGenericException;
import com.xpandit.xray.model.FileStream;
import com.xpandit.xray.model.UploadResult;
import com.xpandit.xray.service.XrayTestImporter;
import com.xpandit.xray.service.impl.XrayTestImporterCloudImpl;
import com.xpandit.xray.service.impl.XrayTestImporterImpl;
import com.xpandit.xray.service.impl.delegates.HttpRequestProvider;
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
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.entity.ContentType;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * This class is responsible for performing the Xray: Cucumber Features Import Task
 */
public class XrayImportFeatureBuilder extends Builder implements SimpleBuildStep {
    
    private static final String TMP_ZIP_FILENAME = "xray_cucumber_features.zip";

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

        listener.getLogger().println("Starting XRAY: Cucumber Features Import Task...");

        listener.getLogger().println("##########################################################");
        listener.getLogger().println("####   Xray is importing the feature files  ####");
        listener.getLogger().println("##########################################################");

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
        
        final HttpRequestProvider.ProxyBean proxyBean = ProxyUtil.createProxyBean();
        XrayTestImporter client;
        if (xrayInstance.getHosting() == HostingType.CLOUD) {
            client = new XrayTestImporterCloudImpl(xrayInstance.getCredential(run).getUsername(), xrayInstance.getCredential(run).getPassword(), proxyBean);
        } else if (xrayInstance.getHosting() == null || xrayInstance.getHosting() == HostingType.SERVER) {
            client = new XrayTestImporterImpl(xrayInstance.getServerAddress(),
                    xrayInstance.getCredential(run).getUsername(),
                    xrayInstance.getCredential(run).getPassword(),
                    proxyBean);
        } else {
            throw new XrayJenkinsGenericException("Hosting type not recognized.");
        }

        processImport(workspace, client, listener);

    }

    private void processImport(final FilePath workspace,
                          final XrayTestImporter client,
                          final TaskListener listener) throws IOException, InterruptedException {
        
        try{
            final Set<String> validFilePath = FileUtils.getFeatureFileNamesFromWorkspace(workspace, this.folderPath, listener);
            final FilePath zipFile = createZipFile(workspace);
            
            // Create Zip file in the workspace's root folder
            workspace.zip(zipFile.write(), new OnlyFeatureFilesInPathFilter(validFilePath, lastModified));

            // Uploads the Zip file to the Jira instance
            uploadZipFile(client, listener, zipFile);
            
            // Deletes the Zip File
            deleteFile(zipFile, listener);

        } catch(XrayClientCoreGenericException  e){
            listener.error(e.getMessage());
            throw new AbortException(e.getMessage());
        } finally {
            client.shutdown();
        }

    }
    
    private void deleteFile(FilePath file, TaskListener listener) throws IOException, InterruptedException {
        try {
            file.delete();
            listener.getLogger().println("Temporary file: " + file.getRemote() + " deleted");
        } catch (IOException | InterruptedException e) {
            listener.getLogger().println("Unable to delete temporary file: " + file.getRemote());
            throw e;
        }
    }

    private void uploadZipFile(XrayTestImporter client, TaskListener listener, FilePath zipFile) throws IOException, InterruptedException {
        FileStream zipFileStream = new FileStream(
                zipFile.getName(),
                zipFile.read(),
                ContentType.APPLICATION_JSON);
        UploadResult uploadResult = client.importFeatures(this.projectKey, zipFileStream);
        listener.getLogger().println(uploadResult.getMessage());
    }

    private FilePath createZipFile(final FilePath workspace) {
        return new FilePath(workspace, TMP_ZIP_FILENAME);
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
