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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xpandit.plugins.xrayjenkins.model.ServerConfiguration;
import com.xpandit.plugins.xrayjenkins.model.XrayInstance;
import com.xpandit.xray.exception.XrayClientCoreGenericException;
import com.xpandit.xray.model.Content;
import com.xpandit.xray.model.Endpoint;
import com.xpandit.xray.model.FormatBean;
import com.xpandit.xray.service.XrayImporter;
import com.xpandit.xray.service.impl.XrayImporterImpl;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
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
public class XrayImportBuilder extends Notifier implements SimpleBuildStep {
    
    private XrayInstance xrayInstance;
    private Endpoint endpoint;
    private Map<String,String> dynamicFields;
    
    private String formatSuffix; //value of format select
    private String serverInstance;//Configuration ID of the JIRA instance
    private String inputInfoSwitcher;//value of the input type switcher
    
    
    private static Gson gson = new GsonBuilder().create();
    
    public XrayImportBuilder(XrayInstance xrayInstance, Endpoint endpoint, Map<String, String> dynamicFields) {
    	this.xrayInstance = xrayInstance;
    	this.endpoint = endpoint;
    	this.dynamicFields = dynamicFields;
    	
    	this.formatSuffix = endpoint.getSuffix();
    	this.serverInstance = xrayInstance.getConfigID();
    	this.inputInfoSwitcher = dynamicFields.get("inputInfoSwitcher");
	}
	
    
    public Map<String,String> getDynamicFields(){
    	return dynamicFields;
    }
    
    public String getFormatSuffix(){
    	return formatSuffix;
    }
    
    public String getServerInstance(){
    	return serverInstance;
    }
    
    public void setServerInstance(String serverInstance){
    	this.serverInstance = serverInstance;
    }
   
    public void setFormatSuffix(String formatSuffix){
    	this.formatSuffix = formatSuffix;
    }
	
	public Endpoint getEndpoint(){
		return this.endpoint;
	}
	
	public String getFormatName(){
		return this.endpoint.getName();
	}
	
	public String getInputInfoSwitcher() {
		return inputInfoSwitcher;
	}

	public void setInputInfoSwitcher(String inputInfoSwitcher) {
		this.inputInfoSwitcher = inputInfoSwitcher;
	}
	
	public String defaultFormats(){
        Map<String,FormatBean> formats = new HashMap<String,FormatBean>();
        for(Endpoint e : Endpoint.values()){
        	FormatBean bean = e.toBean();
        	formats.put(e.getSuffix(),bean);
        	
        	if(e.name().equals(endpoint.name()))
        		bean.setFieldsConfiguration(dynamicFields);
        }
        return gson.toJson(formats);	
    }
    
	public File getReportFile(FilePath workspace, String filePath,TaskListener listener) throws IOException{
		File file = getFile(workspace,filePath,listener);
		if(file.isDirectory() || !file.exists()){
            throw new IOException("File path is a directory or the file doesn't exist");
        }
		return file;
	}
	
	public File getFile(FilePath workspace, String filePath,TaskListener listener) throws IOException{
		   File f = new File(workspace.getRemote(), filePath);
		   listener.getLogger().println("File: "+f.getAbsolutePath());
		   return f;
	}
	
	private String expand(EnvVars environment, String variable){
		if(StringUtils.isNotBlank(variable)){
			String expanded = environment.expand(variable);
			return expanded.equals(variable) ? variable : expanded;
		}
		return null;
	}

	@Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener)throws AbortException, InterruptedException, IOException {
		validate(dynamicFields);
		
        listener.getLogger().println("Starting import task...");
        
        listener.getLogger().println("Import Cucumber features Task started...");

        listener.getLogger().println("##########################################################");
        listener.getLogger().println("####   Xray for JIRA is importing the feature files  ####");
        listener.getLogger().println("##########################################################");

        XrayImporter client = new XrayImporterImpl(xrayInstance.getServerAddress(),xrayInstance.getUsername(),xrayInstance.getPassword());

        try {
        	EnvVars env = build.getEnvironment(listener);
        	
	    	Map<com.xpandit.xray.model.QueryParameter,String> queryParams = new HashMap<com.xpandit.xray.model.QueryParameter,String>();
	          
	        String projectKey = dynamicFields.get(com.xpandit.xray.model.QueryParameter.PROJECT_KEY.getKey());
	        queryParams.put(com.xpandit.xray.model.QueryParameter.PROJECT_KEY, this.expand(env,projectKey));
	          
	        String testExecKey = dynamicFields.get(com.xpandit.xray.model.QueryParameter.TEST_EXEC_KEY.getKey());
	        queryParams.put(com.xpandit.xray.model.QueryParameter.TEST_EXEC_KEY, this.expand(env,testExecKey));
	          
	        String testPlanKey = dynamicFields.get(com.xpandit.xray.model.QueryParameter.TEST_PLAN_KEY.getKey());
	        queryParams.put(com.xpandit.xray.model.QueryParameter.TEST_PLAN_KEY, this.expand(env,testPlanKey));
	          
	        String testEnvironments = dynamicFields.get(com.xpandit.xray.model.QueryParameter.TEST_ENVIRONMENTS.getKey());
	        queryParams.put(com.xpandit.xray.model.QueryParameter.TEST_ENVIRONMENTS, this.expand(env,testEnvironments));
	          
	        String revision = dynamicFields.get(com.xpandit.xray.model.QueryParameter.REVISION.getKey());
	        queryParams.put(com.xpandit.xray.model.QueryParameter.REVISION, this.expand(env,revision));
        	
        	String importFilePath = dynamicFields.get(com.xpandit.xray.model.DataParameter.FILEPATH.getKey());
        	String importInfo = dynamicFields.get(com.xpandit.xray.model.DataParameter.INFO.getKey());
            
            Map<com.xpandit.xray.model.DataParameter,Content> dataParams = new HashMap<com.xpandit.xray.model.DataParameter,Content>();
            
            if(StringUtils.isNotBlank(importFilePath)){
            	String resolved = this.expand(env,importFilePath);
            	Content results = new com.xpandit.xray.model.FilePath(getReportFile(workspace,resolved,listener).getAbsolutePath(),
            														endpoint.getResultsMediaType());
            	dataParams.put(com.xpandit.xray.model.DataParameter.FILEPATH, results);
            }
            if(StringUtils.isNotBlank(importInfo)){
            	String resolved = this.expand(env,importInfo);
            	String inputInfoSwitcher = dynamicFields.get("inputInfoSwitcher");
	            Content info = inputInfoSwitcher.equals("filePath") ? 
	            		new com.xpandit.xray.model.FilePath(getFile(workspace,resolved,listener).getAbsolutePath(),endpoint.getInfoFieldMediaType()) :
	            		new com.xpandit.xray.model.StringContent(resolved, endpoint.getInfoFieldMediaType());
    		    dataParams.put(com.xpandit.xray.model.DataParameter.INFO, info);
            }
       
            client.uploadResults(endpoint, dataParams, queryParams);
            listener.getLogger().println("Sucessfully imported "+endpoint.getName()+" results");
            
        } catch(XrayClientCoreGenericException e){
        	e.printStackTrace();
        	listener.getLogger().println("Task failed");
        	listener.error(e.getMessage());
        	throw new AbortException(e.getMessage());
        }catch (InterruptedException e) {
			e.printStackTrace();
			listener.getLogger().println("Task failed");
			listener.error(e.getMessage());
			throw new InterruptedException(e.getMessage());
		}catch (IOException e) {
            e.printStackTrace();
            listener.getLogger().println("Task failed");
            listener.error(e.getMessage());
            throw new IOException(e);
        }
    }
	
    private void validate(Map<String,String> dynamicFields) throws FormValidation{
      	 
      	 for(com.xpandit.xray.model.DataParameter dp : com.xpandit.xray.model.DataParameter.values()){ // TODO useless
      		 if(dynamicFields.containsKey(dp.getKey()) && dp.isRequired()){
      			 String value = dynamicFields.get(dp.getKey());
      			 if(StringUtils.isBlank(value))
      				throw FormValidation.error("You must configure the field "+dp.getLabel());
      		 }
      	 }
      	 
      	for(com.xpandit.xray.model.QueryParameter qp : com.xpandit.xray.model.QueryParameter.values()){
      		 if(dynamicFields.containsKey(qp.getKey()) && qp.isRequired()){
      			 String value = dynamicFields.get(qp.getKey());
      			 if(StringUtils.isBlank(value))
      				throw FormValidation.error("You must configure the field "+qp.getLabel());
      		 }
      	 }
      	 
      	 String importFilePath = dynamicFields.get(com.xpandit.xray.model.DataParameter.FILEPATH.getKey());

      	 if(importFilePath.contains("../"))
      		throw FormValidation.error("You cannot provide file paths for upper directories.");
   }
    
	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
	
	
	@Extension
    public static class Descriptor extends BuildStepDescriptor<Publisher> {
        private static long BUILD_STEP_SEED = 0;
        private long buildID;
                
        public Descriptor() {
        	super(XrayImportBuilder.class);
            load();
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            
            save();
            return true;
            
        }
        
        @Override
		public XrayImportBuilder newInstance(StaplerRequest req, JSONObject formData){

        	Map<String,String> dynamicFields = getDynamicFields(formData.getJSONObject("dynamicFields"));
			XrayInstance server = getConfiguration(formData.getString("serverInstance"));
			Endpoint endpoint = Endpoint.lookupBySuffix(formData.getString("formatSuffix"));
			
			return new XrayImportBuilder(server,endpoint,dynamicFields);
			
        }
        
        private Map<String,String> getDynamicFields(JSONObject configuredFields){
        	
        	Map<String,String> dynamicFields = new HashMap<String,String>();
        	
        	Set<String> keys = configuredFields.keySet();
        	
        	for(String key : keys){
        		if(configuredFields.containsKey(key)){
        			String value = configuredFields.getString(key);
					if(StringUtils.isNotBlank(value))
						dynamicFields.put(key, value);
        		}
        	}
        	
        	return dynamicFields;
        	
        }
        
        
        private XrayInstance getConfiguration(String configID){
        	XrayInstance config =  null;
        	List<XrayInstance> serverInstances =  getServerInstances();
        	for(XrayInstance sc : serverInstances){
        		if(sc.getConfigID().equals(configID)){
        			config = sc;break;
        		}
        	}
        	return config;
        }
   
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return FreeStyleProject.class.isAssignableFrom(jobType);
        }

        @Override
        public String getDisplayName() {
            return "Xray: Results Import Task";
        }
   
        public ListBoxModel doFillFormatSuffixItems() {
        	
            ListBoxModel items = new ListBoxModel();
            for(Endpoint e : Endpoint.values())
            	items.add(e.getName(), e.getSuffix());
            
            return items;
        }
        
        public ListBoxModel doFillServerInstanceItems() {
        	
            ListBoxModel items = new ListBoxModel();
            List<XrayInstance> serverInstances =  getServerInstances();
            for(XrayInstance sc : serverInstances)
            	items.add(sc.getAlias(),sc.getConfigID());
            
            return items;
        }
        
        public long defaultBuildID(){
        	return buildID;
        }
        
        public void setBuildID(){
        	buildID = ++BUILD_STEP_SEED;
        }
        
        public String defaultFormats(){
            Map<String,FormatBean> formats = new HashMap<String,FormatBean>();
            for(Endpoint e : Endpoint.values()){
            	FormatBean bean = e.toBean();
            	formats.put(e.getSuffix(),bean);
            }
            return gson.toJson(formats);	
        }
        
        public List<XrayInstance> getServerInstances() {
			return ServerConfiguration.get().getServerInstances();
		}
        
    }




}
