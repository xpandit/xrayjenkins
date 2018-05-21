/**
 * XP.RAVEN Project
 * <p>
 * Copyright (C) 2016 Xpand IT.
 * <p>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.task;


import com.xpandit.plugins.xrayjenkins.Utils.ConfigurationUtils;
import org.apache.commons.lang.StringUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.xpandit.plugins.xrayjenkins.exceptions.XrayJenkinsGenericException;

import org.kohsuke.stapler.DataBoundConstructor;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class description.
 *
 * @author <a href="mailto:sebastiao.maya@xpand-it.com">sebastiao.maya</a>
 * @version $Revision: 666 $
 *
 */
public class XrayImportBuilder extends Notifier implements SimpleBuildStep{

	private static final Logger LOG = LoggerFactory.getLogger(XrayImportBuilder.class);
	private static Gson gson = new GsonBuilder().create();
    
    private XrayInstance xrayInstance;
    private Endpoint endpoint;
    private Map<String,String> dynamicFields;
    
    private String formatSuffix; //value of format select
    private String serverInstance;//Configuration ID of the JIRA instance
    private String inputInfoSwitcher;//value of the input type switcher

	/**
	 * Variables used for DataBoundConstructor (Pipeline usage)
	 */
	private String projectKey;
	private String testEnvironments;
	private String testPlanKey;
	private String fixVersion;
	private String importFilePath;
	private String testExecKey;
	private String revision;
	private String importInfo;

    public XrayImportBuilder(XrayInstance xrayInstance,
							 Endpoint endpoint,
							 Map<String, String> dynamicFields) {
    	this.xrayInstance = xrayInstance;
    	this.endpoint = endpoint;
    	this.dynamicFields = dynamicFields;
    	
    	this.formatSuffix = endpoint.getSuffix();
    	this.serverInstance = xrayInstance.getConfigID();
    	this.inputInfoSwitcher = dynamicFields.get("inputInfoSwitcher");
	}

	/**
	 * Constructor used in pipelines projects
	 * "Anyway code run from Pipeline should take any configuration values as literal strings
	 * and make no attempt to perform variable substitution"
	 * @see <a href="https://jenkins.io/doc/developer/plugin-development/pipeline-integration/">Writing Pipeline-Compatible Plugins </a>
	 *
	 * @param serverInstance the server configuration id
	 * @param endpoint the endpoint to be used
	 * @param projectKey the project key
	 * @param testEnvironments the test environments
	 * @param testPlanKey the test plan key
	 * @param fixVersion the fix version
	 * @param importFilePath the path of the result file to be imported
	 * @param testExecKey the test execution key
	 * @param revision the revision
	 * @param importInfo the importation info file or json content
	 * @param inputInfoSwitcher filePath or fileContent switcher
	 */
	@DataBoundConstructor
	public XrayImportBuilder(String serverInstance,
							 String endpoint,
							 String projectKey,
							 String testEnvironments,
							 String testPlanKey,
							 String fixVersion,
							 String importFilePath,
							 String testExecKey,
							 String revision,
							 String importInfo,
							 String inputInfoSwitcher){
    	this.serverInstance = serverInstance;
    	this.endpoint = endpoint != null ? getEndpointValue(endpoint) : null;
    	this.xrayInstance = ConfigurationUtils.getConfiguration(serverInstance);
		this.formatSuffix = this.endpoint != null ? this.endpoint.getSuffix() : null;

		setDynamicFields(projectKey,
				testEnvironments,
				testPlanKey,
				fixVersion,
				importFilePath,
				testExecKey,
				revision,
				importInfo,
				inputInfoSwitcher);

		this.inputInfoSwitcher = dynamicFields.get("inputInfoSwitcher");
	}

	private Endpoint getEndpointValue(String endpoint){
		switch(endpoint){
			case "xrayjson":
				return Endpoint.XRAY;
			case "cucumber":
				return Endpoint.CUCUMBER;
			case "cucumbermultipart":
				return Endpoint.CUCUMBER_MULTIPART;
			case "behave":
				return Endpoint.BEHAVE;
			case "behavemultipart":
				return Endpoint.BEHAVE_MULTIPART;
			case "junit":
				return Endpoint.JUNIT;
			case "junitmultipart":
				return Endpoint.JUNIT_MULTIPART;
			case "nunit":
				return Endpoint.NUNIT;
			case "nunitmultipart":
				return Endpoint.NUNIT_MULTIPART;
			case "robot":
				return Endpoint.ROBOT;
			case "robotmultipart":
				return Endpoint.ROBOT_MULTIPART;
			case "bundle":
				return Endpoint.BUNDLE;
			default:
				LOG.error("provided Xray Endpoint is not valid");
				return null;
		}
	}

	private void setDynamicFields(String projectKey,
								  String testEnvironments,
								  String testPlanKey,
								  String fixVersion,
								  String importFilePath,
								  String testExecKey,
								  String revision,
								  String importInfo,
								  String inputInfoSwitcher){
    	if(dynamicFields == null){
    		dynamicFields = new HashMap<>();
		}
    	if (!StringUtils.isBlank(projectKey)){
    		dynamicFields.put("projectKey",projectKey);
		}
		if (!StringUtils.isBlank(testEnvironments)){
			dynamicFields.put("testEnvironments",testEnvironments);
		}
		if (!StringUtils.isBlank(testPlanKey)){
			dynamicFields.put("testPlanKey",testPlanKey);
		}
		if (!StringUtils.isBlank(fixVersion)){
			dynamicFields.put("fixVersion",fixVersion);
		}
		if (!StringUtils.isBlank(importFilePath)){
			dynamicFields.put("importFilePath",importFilePath);
		}
		if (!StringUtils.isBlank(testExecKey)){
			dynamicFields.put("testExecKey",testExecKey);
		}
		if (!StringUtils.isBlank(projectKey)){
			dynamicFields.put("revision",revision);
		}
		if(!StringUtils.isBlank(importInfo)){
    		dynamicFields.put("importInfo", importInfo);
		}
		if(!StringUtils.isBlank(inputInfoSwitcher)){
    		dynamicFields.put("inputInfoSwitcher", inputInfoSwitcher);
		}
	}

	public String getProjectKey() {
		return projectKey;
	}

	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}

	public String getTestEnvironments() {
		return testEnvironments;
	}

	public void setTestEnvironments(String testEnvironments) {
		this.testEnvironments = testEnvironments;
	}

	public String getTestPlanKey() {
		return testPlanKey;
	}

	public void setTestPlanKey(String testPlanKey) {
		this.testPlanKey = testPlanKey;
	}

	public String getFixVersion() {
		return fixVersion;
	}

	public void setFixVersion(String fixVersion) {
		this.fixVersion = fixVersion;
	}

	public String getImportFilePath() {
		return importFilePath;
	}

	public void setImportFilePath(String importFilePath) {
		this.importFilePath = importFilePath;
	}

	public String getTestExecKey() {
		return testExecKey;
	}

	public void setTestExecKey(String testExecKey) {
		this.testExecKey = testExecKey;
	}

	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	public String getImportInfo() {
		return importInfo;
	}

	public void setImportInfo(String importInfo) {
		this.importInfo = importInfo;
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
    
	private FilePath getFile(FilePath workspace, String filePath,TaskListener listener) throws IOException, InterruptedException{
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
	
	private FilePath readFile(FilePath workspace, String filePath, TaskListener listener) throws IOException{
		   FilePath f = new FilePath(workspace, filePath);
		   listener.getLogger().println("File: "+f.getRemote());
		   return f;
	}
	
	private String expand(EnvVars environment, String variable){
		if(StringUtils.isNotBlank(variable)){
			String expanded = environment.expand(variable);
			return expanded.equals(variable) ? variable : expanded;
		}
		return "";
	}

	@Override
    public void perform(Run<?,?> build,
						FilePath workspace,
						Launcher launcher,
						TaskListener listener)throws AbortException, InterruptedException, IOException {
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
	        
	        String fixVersion = dynamicFields.get(com.xpandit.xray.model.QueryParameter.FIX_VERSION.getKey());
	        queryParams.put(com.xpandit.xray.model.QueryParameter.FIX_VERSION, this.expand(env,fixVersion));
        	
        	String importFilePath = dynamicFields.get(com.xpandit.xray.model.DataParameter.FILEPATH.getKey());
        	String importInfo = dynamicFields.get(com.xpandit.xray.model.DataParameter.INFO.getKey());
            
            Map<com.xpandit.xray.model.DataParameter,Content> dataParams = new HashMap<>();
            
            if(StringUtils.isNotBlank(importFilePath)){
            	String resolved = this.expand(env,importFilePath);
				FilePath resultsFile = getFile(workspace,resolved,listener);

				Content results = new com.xpandit.xray.model.FileStream(resultsFile.getName(),resultsFile.read(),
            																endpoint.getResultsMediaType());

            	dataParams.put(com.xpandit.xray.model.DataParameter.FILEPATH, results);
            }
            if(StringUtils.isNotBlank(importInfo)){
            	String resolved = this.expand(env,importInfo);
            	String inputInfoSwitcher = dynamicFields.get("inputInfoSwitcher");

				Content info;
				if(inputInfoSwitcher.equals("filePath")){
					FilePath infoFile = getFile(workspace,resolved,listener);
					info = new com.xpandit.xray.model.FileStream(infoFile.getName(),infoFile.read(),endpoint.getInfoFieldMediaType());
				}else{
					info = new com.xpandit.xray.model.StringContent(resolved, endpoint.getInfoFieldMediaType());
				}

    		    dataParams.put(com.xpandit.xray.model.DataParameter.INFO, info);
            }

			client.uploadResults(endpoint, dataParams, queryParams);

            listener.getLogger().println("Sucessfully imported "+endpoint.getName()+" results");
            
        }catch(XrayClientCoreGenericException e){
        	e.printStackTrace();
        	throw new AbortException(e.getMessage());
        }catch(XrayJenkinsGenericException e){
            e.printStackTrace();
            throw new AbortException(e.getMessage());
        }catch (IOException e) {
            e.printStackTrace();
            listener.error(e.getMessage());
            throw new IOException(e);
        }finally{
			client.shutdown();
		}
	}

    private void validate(Map<String,String> dynamicFields) throws FormValidation{

		if(serverInstance == null){
			LOG.error("configuration id is null");
			throw new XrayJenkinsGenericException("configuration id is null");
		}
		if(xrayInstance == null){
			LOG.error("Xray instance is null");
			throw new XrayJenkinsGenericException("Xray instance is null");
		}
		if(endpoint == null){
			LOG.error("passed endpoint is null or could not be found");
			throw new XrayJenkinsGenericException("passed endpoint is null or could not be found");
		}
      	 for(com.xpandit.xray.model.DataParameter dp : com.xpandit.xray.model.DataParameter.values()){
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
			XrayInstance server = ConfigurationUtils.getConfiguration(formData.getString("serverInstance"));
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
        
        
        /*private XrayInstance getConfiguration(String configID){
        	XrayInstance config =  null;
        	List<XrayInstance> serverInstances =  getServerInstances();
        	for(XrayInstance sc : serverInstances){
        		if(sc.getConfigID().equals(configID)){
        			config = sc;break;
        		}
        	}
        	return config;
        }*/
   
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
        	LOG.info("~~~~~~jobtype is: {}", jobType);
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
