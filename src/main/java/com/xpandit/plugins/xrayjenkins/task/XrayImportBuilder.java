/*
 * xray-jenkins Project
 *
 * Copyright (C) 2016 Xpand IT.
 *
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.task;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xpandit.plugins.xrayjenkins.model.Format;
import com.xpandit.plugins.xrayjenkins.model.XrayInstance;
import com.xpandit.plugins.xrayjenkins.service.XrayRestClient;
import com.xpandit.plugins.xrayjenkins.model.FormatBean;
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
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

/**
 * Class description.
 *
 * @author <a href="mailto:sebastiao.maya@xpand-it.com">sebastiao.maya</a>
 * @version $Revision: 666 $
 *
 */
public class XrayImportBuilder extends Builder implements SimpleBuildStep {

    private static final String IMPORT_CUCUMBER_FEATURES_URL_SUFIX= "/rest/raven/1.0/import/execution";

    private final String serverUrl;
    private final String serverUsername;
    private final String serverPassword;
    private final String format;
    private String importFilePath;
    private String formats;
    
    private static Gson gson = new GsonBuilder().create();
    
    @DataBoundConstructor
    public XrayImportBuilder(String serverUrl,String serverUsername, 
            String serverPassword, String format, String importFilePath) {
        this.serverUrl = serverUrl;
        this.serverUsername = serverUsername;
        this.serverPassword = serverPassword;
        this.format = format;
        this.importFilePath = importFilePath;
        Map<String,FormatBean> formats = new HashMap<String,FormatBean>();
        
        for(Format f: Format.values()){ //Populate with the defined formats
        	FormatBean formatBean = createFormatBean(f);//A temporary bean to hold necessary info
        
        	//if(formatConfiguration.equals(format.getSuffix()))//populate the dynamic fields given the last chosen format
        		//formatBean.setFieldsConfiguration(taskDefinition.getConfiguration());
        		
        	formats.put(f.getName(),formatBean); 
        }
        this.formats = gson.toJson(formats);
        
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

    public String getImportFilePath() {
        return importFilePath;
    }

    public String getFormat() {
        return format;
    }
    
    public String getFormats(){
    	return formats;
    }
    
    /**
     * Creates a Bean
     * @param format Format definition
     * @param i18nResolver i18n property resolver
     * @return Bean
     */
    private FormatBean createFormatBean(Format format){
    	return new FormatBean(format.getName(), format.getSuffix(),format.getOptionalFields());
    }
    

    @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
        
        listener.getLogger().println("Starting import task...");
        
        listener.getLogger().println("Import Cucumber features Task started...");

        listener.getLogger().println("##########################################################");
        listener.getLogger().println("####   Xray for JIRA is importing the feature files  ####");
        listener.getLogger().println("##########################################################");

        XrayInstance xrayInstance = new XrayInstance(serverUrl,serverUsername,serverPassword);
        XrayRestClient client = XrayRestClient.createXrayRestClient(xrayInstance);

        try {
            if (importFilePath == null || StringUtils.isEmpty(importFilePath)) {
                importFilePath = "features/result.json";
            }
            
            File file = new File(workspace.getRemote(), importFilePath);
            if(file.isDirectory() || !file.exists()){
                listener.getLogger().println("Import Cucumber features Task failed.");
                throw new IOException("File path is a directory or the file doesn't exist");
            }
            
            String restCall = IMPORT_CUCUMBER_FEATURES_URL_SUFIX + format;
            
            HttpResponse response = client.httpPost(restCall, file.getAbsolutePath());

            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != 200) {
                try {
                    throw new ClientProtocolException("Unexpected response status: " + statusCode);
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {
        
        private String serverUrl;
        private String serverUsername;
        private String serverPassword;
        private String format;
        private String importFilePath;
        
        public Descriptor() {
            load();
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            
            serverUrl = formData.getString("serverUrl");
            serverUsername = formData.getString("serverUsername");
            serverPassword = formData.getString("serverPassword");
            format = formData.getString("format");
            importFilePath = formData.getString("importFilePath");
            		
            save();
            return super.configure(req,formData);
        }
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return FreeStyleProject.class.isAssignableFrom(jobType);
        }

        @Override
        public String getDisplayName() {
            return "Xray Import task";
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
        public FormValidation doCheckImportFilePath(@QueryParameter String value) {

            if(value.contains("../")){
                return FormValidation.error("You can't provide file paths for upper directories.Please don't use \"../\".");
            }
            else{
                return FormValidation.ok();
            }
        }
        
        public ListBoxModel doFillFormatItems() {
            ListBoxModel items = new ListBoxModel();
            
            for(Format format : Format.values())
            	items.add(format.getName(), format.getSuffix());
  
            return items;
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

        public String getFormat() {
            return format;
        }

        public String getImportFilePath() {
            return importFilePath;
        }
        
        public String defaultValue(){     
        	return "hello boss";
        }
        
    }

}
