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
import com.xpandit.plugins.xrayjenkins.model.XrayInstance;
import com.xpandit.plugins.xrayjenkins.service.XrayRestClient;
import com.xpandit.xray.model.Endpoint;
import com.xpandit.xray.model.FormatBean;

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
    private final String formatSuffix;
    private String formatName;
    private String formats;
    private String importFilePath;
    private static Gson gson = new GsonBuilder().create();
    
    @DataBoundConstructor
    public XrayImportBuilder(String serverUrl,String serverUsername, 
            String serverPassword, String formatSuffix, String importFilePath) {
    	
    	
        this.serverUrl = serverUrl;
        this.serverUsername = serverUsername;
        this.serverPassword = serverPassword;
        this.importFilePath = importFilePath;
        this.formatSuffix = formatSuffix;
        this.formatName = Endpoint.lookupBySuffix(formatSuffix).getName();
        
        //TODO only save if changes ocurred in Endpoints Enum
        Map<String,FormatBean> formats = new HashMap<String,FormatBean>();
        for(Endpoint e : Endpoint.values())
        	formats.put(e.getName(),e.toBean());
        
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

    public String getFormatSuffix() {
        return formatSuffix;
    }
    
    public String getFormatName(){
    	return formatName;
    }
    
    public String getFormats(){
    	return this.formats;
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
            
            String restCall = IMPORT_CUCUMBER_FEATURES_URL_SUFIX + formatSuffix;
            
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
        private static int SEED = 0;
    	private String serverUrl;
        private String serverUsername;
        private String serverPassword;
        private String importFilePath;
        private String formatSuffix;
        private String formatName;
        
        
        public Descriptor() {
            load();
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            this.serverUrl = formData.getString("serverUrl");
            this.serverUsername = formData.getString("serverUsername");
            this.serverPassword = formData.getString("serverPassword");
            this.formatSuffix = formData.getString("formatSuffix");
            this.importFilePath = formData.getString("importFilePath");
            
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
        
        public ListBoxModel doFillFormatSuffixItems() {
        	
            ListBoxModel items = new ListBoxModel();
            for(Endpoint e : Endpoint.values())
            	items.add(e.getName(), e.getSuffix());
            
            return items;
        }
        
        public ListBoxModel doFillDefaultFormatItems(){
        	
        	ListBoxModel items = new ListBoxModel();
        	
            Map<String,FormatBean> formats = new HashMap<String,FormatBean>();
            for(Endpoint e : Endpoint.values())
            	formats.put(e.getName(),e.toBean());
        	
            items.add(gson.toJson(formats));
            
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

        public String getImportFilePath() {
            return importFilePath;
        }
        
        public String getFormatSuffix() {
            return formatSuffix;
        }
        
        public String getFormatName(){
        	return formatName;
        }
        
        public int defaultUid(){
        	return ++SEED;
        }
        
        public String defaultFormats(){
            Map<String,FormatBean> formats = new HashMap<String,FormatBean>();
            for(Endpoint e : Endpoint.values())
            	formats.put(e.getName(),e.toBean());
            
            return gson.toJson(formats);	
        }
        
        public String defaultValue(){ 
        	//Endpoint e = Endpoint.lookupBySuffix(this.formatSuffix);
        	//return e != null ? e.getName() : "";
        	
        	return this.formatSuffix;
        }
        
    }

}
