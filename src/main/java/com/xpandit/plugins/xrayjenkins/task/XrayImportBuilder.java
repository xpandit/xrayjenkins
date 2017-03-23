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
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xpandit.plugins.xrayjenkins.model.XrayInstance;
import com.xpandit.plugins.xrayjenkins.service.XrayRestClient;
import com.xpandit.xray.exception.XrayClientCoreGenericException;
import com.xpandit.xray.model.Content;
import com.xpandit.xray.model.DataParameter;
import com.xpandit.xray.model.Endpoint;
import com.xpandit.xray.model.FormatBean;
import com.xpandit.xray.service.XrayImporter;
import com.xpandit.xray.service.impl.XrayImporterImpl;
import hudson.EnvVars;
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

    private final String serverUrl;
    private final String serverUsername;
    private final String serverPassword;
   
    private String formatSuffix;
    private String formatName;
    private String importFilePath;
    private String importInfo;
    private String projectKey;
    private String testExecKey;
    private String testPlanKey;
    private String testEnvironments;
    private String revision;
    private String inputInfoSwitcher;
    
    private Endpoint endpoint;
    
    private static Gson gson = new GsonBuilder().create();
    
    @DataBoundConstructor
    public XrayImportBuilder(String serverUrl,String serverUsername, 
            String serverPassword, String formatSuffix, 
            String importFilePath) {
        this.serverUrl = serverUrl;
        this.serverUsername = serverUsername;
        this.serverPassword = serverPassword;
        this.formatSuffix = formatSuffix;
        this.endpoint = Endpoint.lookupBySuffix(formatSuffix);
        this.formatName = endpoint.getName();   
        this.importFilePath = importFilePath;
    }
    
    @DataBoundSetter
    public void setImportInfo(String importInfo){
    	this.importInfo = importInfo;
    }
    
    @DataBoundSetter
    public void setProjectKey(String projectKey){
    	this.projectKey = projectKey;
    }
    
    @DataBoundSetter
    public void setTestExecKey(String testExecKey){
    	this.testExecKey = testExecKey;
    }
    
    @DataBoundSetter
    public void setTestPlanKey(String testPlanKey){
    	this.testPlanKey = testPlanKey;
    }
    
    @DataBoundSetter
    public void setTestEnvironments(String testEnvironments){
    	this.testEnvironments = testEnvironments;
    }
    
    @DataBoundSetter
    public void setRevision(String revision){
    	this.revision = revision;
    }
    
    @DataBoundSetter
    public void setFormatSuffix(String formatSuffix){
    	this.formatSuffix = formatSuffix;
    }
    
    @DataBoundSetter
    public void setInputInfoSwitcher(String inputInfoSwitcher){
    	this.inputInfoSwitcher = inputInfoSwitcher;
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

    public String getProjectKey() {
		return projectKey;
	}
    
    public String getImportInfo() {
		return importInfo;
	}

	public String getTestExecKey() {
		return testExecKey;
	}

	public String getTestPlanKey() {
		return testPlanKey;
	}

	public String getTestEnvironments() {
		return testEnvironments;
	}

	public String getRevision() {
		return revision;
	}
	
	public String getInputInfoSwitcher(){
		return inputInfoSwitcher;
	}
	
	public Endpoint getEndpoint(){
		return this.endpoint;
	}

	@Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
	
        listener.getLogger().println("Starting import task...");
        
        listener.getLogger().println("Import Cucumber features Task started...");

        listener.getLogger().println("##########################################################");
        listener.getLogger().println("####   Xray for JIRA is importing the feature files  ####");
        listener.getLogger().println("##########################################################");

        XrayInstance xrayInstance = new XrayInstance(serverUrl,serverUsername,serverPassword);
        XrayImporter client = new XrayImporterImpl(xrayInstance.getServerAddress(),xrayInstance.getUsername(),xrayInstance.getPassword());

        try {
        	EnvVars env = build.getEnvironment(listener);
        	
            if (StringUtils.isBlank(importFilePath)) {
                importFilePath = "features/result.json";
            }
            
            Map<com.xpandit.xray.model.DataParameter,Content> dataParams = new HashMap<com.xpandit.xray.model.DataParameter,Content>();
            
            if(StringUtils.isNotBlank(importFilePath)){
            	String resolved = this.expand(env,importFilePath);
            	Content results = new com.xpandit.xray.model.FilePath(getReportFile(workspace,resolved,listener).getAbsolutePath(),
            														endpoint.getResultsMediaType());
            	dataParams.put(DataParameter.FILEPATH, results);
            }
            if(StringUtils.isNotBlank(importInfo)){
            	String resolved = this.expand(env,importInfo);
	            Content info = inputInfoSwitcher.equals("filePath") ? 
	            		new com.xpandit.xray.model.FilePath(getFile(workspace,resolved).getAbsolutePath(),endpoint.getInfoFieldMediaType()) :
	            		new com.xpandit.xray.model.StringContent(resolved, endpoint.getInfoFieldMediaType());
    		    dataParams.put(DataParameter.INFO, info);
            }
            
            Map<com.xpandit.xray.model.QueryParameter,String> queryParams = new HashMap<com.xpandit.xray.model.QueryParameter,String>();
            queryParams.put(com.xpandit.xray.model.QueryParameter.PROJECT_KEY, this.expand(env,projectKey));
            queryParams.put(com.xpandit.xray.model.QueryParameter.TEST_EXEC_KEY, this.expand(env,testExecKey));
            queryParams.put(com.xpandit.xray.model.QueryParameter.TEST_PLAN_KEY, this.expand(env,testPlanKey));
            queryParams.put(com.xpandit.xray.model.QueryParameter.TEST_ENVIRONMENTS, this.expand(env,testEnvironments));
            queryParams.put(com.xpandit.xray.model.QueryParameter.REVISION, this.expand(env,revision));
       
            client.uploadResults(endpoint, dataParams, queryParams);
            listener.getLogger().println("Sucessfully imported "+endpoint.getName()+" results");
            
        } catch(XrayClientCoreGenericException e){
        	e.printStackTrace();
        	listener.getLogger().println(e.getMessage());
        	listener.getLogger().println("Task failed");
        }catch (InterruptedException e) {
			e.printStackTrace();
			listener.getLogger().println(e.getMessage());
		}catch (IOException e) {
            e.printStackTrace();
            listener.getLogger().println(e.getMessage());
            listener.getLogger().println("Task failed");
        }
    }
	
	public File getReportFile(FilePath workspace, String filePath,TaskListener listener) throws IOException{
		File file = getFile(workspace,filePath);
		if(file.isDirectory() || !file.exists()){
            listener.getLogger().println("Import execution results task failed.");
            throw new IOException("File path is a directory or the file doesn't exist");
        }
		return file;
	}
	
	public File getFile(FilePath workspace, String filePath){
		   return new File(workspace.getRemote(), filePath);
	}
	
	private String expand(EnvVars environment, String variable){
		String expanded = environment.expand(variable);
		return expanded.equals(variable) ? variable : expanded;
	}

	@Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {
    	
        private static int SEED = 0;
        private int uid;
    	private String serverUrl;
        private String serverUsername;
        private String serverPassword;
        private String formatSuffix;
        
        private String importFilePath;
        private String importInfo;
        private String projectKey;
        private String testExecKey;
        private String testPlanKey;
        private String testEnvironments;
        private String revision;
        
        public Descriptor() {
        	super(XrayImportBuilder.class);
            load();
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            this.serverUrl = formData.getString("serverUrl");
            this.serverUsername = formData.getString("serverUsername");
            this.serverPassword = formData.getString("serverPassword");
            this.formatSuffix = formData.getString("formatSuffix");
            
            
            
            save();
            //return super.configure(req,formData);
            return true;
        }
        
        public void configureEndpointFields(JSONObject formData){
        	this.importFilePath = formData.getString(com.xpandit.xray.model.DataParameter.FILEPATH.getKey());
        	this.importInfo = formData.getString(com.xpandit.xray.model.DataParameter.INFO.getKey());
        	
        	this.projectKey = formData.getString(com.xpandit.xray.model.QueryParameter.PROJECT_KEY.getKey());
        	this.testExecKey = formData.getString(com.xpandit.xray.model.QueryParameter.TEST_EXEC_KEY.getKey());
        	this.testPlanKey = formData.getString(com.xpandit.xray.model.QueryParameter.TEST_PLAN_KEY.getKey());
        	this.testEnvironments = formData.getString(com.xpandit.xray.model.QueryParameter.TEST_ENVIRONMENTS.getKey());
        	this.revision = formData.getString(com.xpandit.xray.model.QueryParameter.REVISION.getKey());
        			
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
        
        public String getServerUrl() {
            return serverUrl;
        }

        public String getServerUsername() {
            return serverUsername;
        }

        public String getServerPassword() {
            return serverPassword;
        }
        
        public String getFormatSuffix() {
            return formatSuffix;
        }
        
        public int defaultUid(){
        	return this.uid;
        }
        
        public void setUid(){
        	this.uid = ++SEED;
        }
        
        public String defaultFormats(){
            Map<String,FormatBean> formats = new HashMap<String,FormatBean>();
            for(Endpoint e : Endpoint.values()){
            	FormatBean bean = e.toBean();
            	formats.put(e.getName(),bean);
            	
            	//if(e.getSuffix().equals(this.formatSuffix))
            		bean.setFieldsConfiguration(defaultEndpointFields());
            }
            return gson.toJson(formats);	
        }
        
        private Map<String,String> defaultEndpointFields(){
        	Map<String,String> conf = new HashMap<String,String>();
    		
    		conf.put(com.xpandit.xray.model.DataParameter.FILEPATH.getKey(), "hello");
    		conf.put(com.xpandit.xray.model.DataParameter.INFO.getKey(), this.importInfo);
    		
    		conf.put(com.xpandit.xray.model.QueryParameter.PROJECT_KEY.getKey(),this.projectKey);
    		conf.put(com.xpandit.xray.model.QueryParameter.TEST_EXEC_KEY.getKey(),this.testExecKey);
    		conf.put(com.xpandit.xray.model.QueryParameter.TEST_PLAN_KEY.getKey(),this.testPlanKey);
    		conf.put(com.xpandit.xray.model.QueryParameter.TEST_ENVIRONMENTS.getKey(),this.testEnvironments);
    		conf.put(com.xpandit.xray.model.QueryParameter.REVISION.getKey(),this.revision);
    		
    		return conf;
        }
        
        
        
        
        
    }

}
