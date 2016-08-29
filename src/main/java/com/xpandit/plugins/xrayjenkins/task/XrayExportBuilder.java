/*
 * xray-jenkins Project
 *
 * Copyright (C) 2016 Xpand IT.
 *
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.xpandit.plugins.xrayjenkins.model.XrayInstance;
import com.xpandit.plugins.xrayjenkins.service.XrayRestClient;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

/**
 * Class description.
 *
 * @author <a href="mailto:sebastiao.maya@xpand-it.com">sebastiao.maya</a>
 * @version $Revision: 666 $
 *
 */
public class XrayExportBuilder extends Builder implements SimpleBuildStep {

    private static final String EXPORT_CUCUMBER_FEATURES_URL_SUFIX = "/rest/raven/1.0/export/test";

    private final String serverUrl;
    private final String serverUsername;
    private final String serverPassword;
    private final String issues;
    private final String filter;
    private String filePath;

    @DataBoundConstructor
    public XrayExportBuilder(String serverUrl,String serverUsername, 
            String serverPassword, String issues, String filter, String filePath) {
        this.serverUrl = serverUrl;
        this.serverUsername = serverUsername;
        this.serverPassword = serverPassword;
        this.issues = issues;
        this.filter = filter;
        this.filePath = filePath;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getServerUsername() {
        return serverUsername;
    }

    public String getServerPassword() {
        return serverPassword;
    }

    public String getIssues() {
        return issues;
    }

    public String getFilter() {
        return filter;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
        
        listener.getLogger().println("Starting export task...");
        
        listener.getLogger().println("##########################################################");
        listener.getLogger().println("####   Xray for JIRA is exporting the feature files  ####");
        listener.getLogger().println("##########################################################");
        
        XrayInstance xrayInstance = new XrayInstance(serverUrl,serverUsername,serverPassword);
        XrayRestClient client = XrayRestClient.createXrayRestClient(xrayInstance);

        try{
            Map<String, String> queryParams = new HashMap<>();

            if (issues != null && !StringUtils.isEmpty(issues)) {
                listener.getLogger().println(issues);
                queryParams.put("keys", issues);
            }

            if (filter != null && !StringUtils.isEmpty(filter)) {
                listener.getLogger().println(filter);
                queryParams.put("filter", filter);
            }

            HttpResponse response = client.httpGet(EXPORT_CUCUMBER_FEATURES_URL_SUFIX, queryParams);

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                InputStream in = response.getEntity().getContent();

                if (filePath == null || StringUtils.isEmpty(filePath)) {
                    filePath = "features/";
                }

                File outputFile = new File(workspace.getRemote(), filePath);
                outputFile.mkdirs();

                FileOutputStream fos = new FileOutputStream(new File(outputFile, "features.zip"));

                byte[] buffer = new byte[4096];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }

                listener.getLogger().println("###################### Unzipping file ####################");

                ZipInputStream zis = new ZipInputStream(new FileInputStream(workspace.getRemote()+"/"+filePath+"/features.zip"));
                //get the zipped file list entry
                ZipEntry ze = zis.getNextEntry();

                while(ze!=null){

                    String fileName = ze.getName();
                    File newFile = new File(workspace.getRemote(), filePath+"/"+fileName);

                    System.out.println("file unzip : "+ newFile.getAbsoluteFile());

                    new File(newFile.getParent()).mkdirs();

                    fos = new FileOutputStream(newFile);             

                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }

                    fos.close();   
                    ze = zis.getNextEntry();
                }

                zis.closeEntry();
                zis.close();
                listener.getLogger().println("###################### Unzipped file #####################");
            } else {
                try {
                    throw new ClientProtocolException("Unexpected response status: " + statusCode);
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                }
            }
        }
        catch (IOException e) {
            listener.getLogger().println("Error:_" + e);
            e.printStackTrace();
        }
    }

    
    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {
        
        private String serverUrl;
        private String serverUsername;
        private String serverPassword;
        private String issues;
        private String filter;
        private String filePath;
        
        
        public Descriptor() {
            load();
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            
            serverUrl = formData.getString("serverUrl");
            serverUsername = formData.getString("serverUsername");
            serverPassword = formData.getString("serverPassword");
            issues = formData.getString("issues");
            filter = formData.getString("filter");
            filePath = formData.getString("filePath");
            
            save();
            return super.configure(req,formData);
        }
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return FreeStyleProject.class.isAssignableFrom(jobType);
        }

        @Override
        public String getDisplayName() {
            return "Xray Export task";
        }

        public FormValidation doTestConnection(@QueryParameter("serverUrl") final String serverUrl,
                @QueryParameter("serverUsername") final String serverUsername, @QueryParameter("serverPassword") final String serverPassword) throws IOException, ServletException {

            XrayInstance testXrayInstance = new XrayInstance(serverUrl,serverUsername,serverPassword);
            XrayRestClient testXrayRestClient = XrayRestClient.createXrayRestClient(testXrayInstance);

            Boolean isConnectionOk = testXrayRestClient.testConnection();
            if(isConnectionOk){
                return FormValidation.ok("Connection: Success!");
            }
            else{
                return FormValidation.error("Could not establish connection");
            }
        }
        /*
         * Checking if the file path doesn't contain "../"
         */
        public FormValidation doCheckFilePath(@QueryParameter String value) {

            if(value.contains("../")){
                return FormValidation.error("You can't provide file paths for upper directories.Please don't use \"../\".");
            }
            else{
                return FormValidation.ok();
            }
        }

        /*
         * Checking if either issues or filter is filled
         */
        public FormValidation doCheckIssues(@QueryParameter String value, @QueryParameter String filter) {
            if (StringUtils.isEmpty(value) && StringUtils.isEmpty(filter)) {
                return FormValidation.error("You must provide issue keys and/or a filter ID in order to export cucumber features from Xray.");
            }
            else{
                return FormValidation.ok();
            }

        }

        public FormValidation doCheckFilter(@QueryParameter String value, @QueryParameter String issues) {            
            if (StringUtils.isEmpty(value) && StringUtils.isEmpty(issues)) {
                return FormValidation.error("You must provide issue keys and/or a filter ID in order to export cucumber features from Xray.");
            }
            else{
                return FormValidation.ok();
            }
        }
        
        public String getServerUrl() {
            return serverUrl;
        }

        public String getServerUsername() {
            return serverUsername;
        }

        public String getServerPassword() {
            return serverPassword;
        }

        public String getIssues() {
            return issues;
        }

        public String getFilter() {
            return filter;
        }

        public String getFilePath() {
            return filePath;
        }
    }

}
