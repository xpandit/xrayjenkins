/**
 * XP.RAVEN Project
 * <p>
 * Copyright (C) 2016 Xpand IT.
 * <p>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpandit.plugins.xrayjenkins.Utils.FileUtils;
import com.xpandit.xray.model.ParameterBean;
import com.xpandit.xray.model.QueryParameter;
import com.xpandit.plugins.xrayjenkins.Utils.ConfigurationUtils;
import com.xpandit.plugins.xrayjenkins.Utils.FormUtils;
import com.xpandit.xray.model.UploadResult;
import com.xpandit.plugins.xrayjenkins.Utils.BuilderUtils;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.xpandit.plugins.xrayjenkins.exceptions.XrayJenkinsGenericException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.kohsuke.stapler.DataBoundConstructor;
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

    /*private Endpoint endpoint;*/
    private Map<String,String> dynamicFields;
    /*private boolean importToSameExecution;*/

    private String formatSuffix; //value of format select
    private String serverInstance;//Configuration ID of the JIRA instance
    private String inputInfoSwitcher;//value of the input type switcher

	private static final String SAME_EXECUTION_CHECKBOX = "importToSameExecution";
	private static final String INFO_INPUT_SWITCHER = "inputInfoSwitcher";
	private static final String SERVER_INSTANCE = "serverInstance";
	private static final String ERROR_LOG = "Error while performing import tasks";



	private String endpoint;
	private String projectKey;
	private String testEnvironments;
	private String testPlanKey;
	private String fixVersion;
	private String importFilePath;
	private String testExecKey;
	private String revision;
	private String importInfo;
	private String importToSameExecution;
    
    /*public XrayImportBuilder(String serverInstance , Endpoint endpoint, Map<String, String> dynamicFields) {
    	this.endpoint = endpoint;
    	this.dynamicFields = dynamicFields;
		if(dynamicFields.get(SAME_EXECUTION_CHECKBOX) != null){
			this.importToSameExecution = dynamicFields.get(SAME_EXECUTION_CHECKBOX).equals("true") ? true : false;
		} else {
			this.importToSameExecution = false;
		}


    	this.formatSuffix = endpoint.getSuffix();
    	this.serverInstance = serverInstance;
    	this.inputInfoSwitcher = dynamicFields.get(INFO_INPUT_SWITCHER);
	}*/

	/**
	 * Constructor used in pipelines projects
     *
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
							 String inputInfoSwitcher,
							 String importToSameExecution){
    	this.serverInstance = serverInstance;
    	/*this.endpoint = StringUtils.isNotBlank(endpoint) ? Endpoint.lookupByName(endpoint.trim()) : null;*/
    	this.endpoint = endpoint;
   		/*this.formatSuffix = this.endpoint != null ? this.endpoint.getSuffix() : null;*/
   		/*this.formatSuffix = Endpoint.lookupByName(endpoint).getSuffix();//todo - atenção aí, so procuramos o nome nos pipelines. permitir os dois??*/
		this.formatSuffix = Endpoint.lookupByName(endpoint) != null ? Endpoint.lookupByName(endpoint).getSuffix()
				: (Endpoint.lookupBySuffix(endpoint) != null ? Endpoint.lookupBySuffix(endpoint).getSuffix() : null);
   		/*this.importToSameExecution = "true".equals(importToSameExecution);*/

   		this.projectKey = projectKey;
   		this.testEnvironments = testEnvironments;
   		this.testPlanKey = testPlanKey;
   		this.fixVersion = fixVersion;
   		this.importFilePath = importFilePath;
   		this.testExecKey = testExecKey;
   		this.revision = revision;
   		this.importInfo = importInfo;
   		this.inputInfoSwitcher = inputInfoSwitcher;
		this.importToSameExecution = importToSameExecution;

		setDynamicFields(projectKey,
				testEnvironments,
				testPlanKey,
				fixVersion,
				importFilePath,
				testExecKey,
				revision,
				importInfo,
				inputInfoSwitcher);

		this.inputInfoSwitcher = dynamicFields.get(INFO_INPUT_SWITCHER);
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
    		dynamicFields.put(INFO_INPUT_SWITCHER, inputInfoSwitcher);
		}
	}

	/*public Map<String,String> getDynamicFields(){
    	return dynamicFields;
    }*/

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

	public static Logger getLOG() {
		return LOG;
	}

	public static Gson getGson() {
		return gson;
	}

	public static void setGson(Gson gson) {
		XrayImportBuilder.gson = gson;
	}

	/*public void setDynamicFields(Map<String, String> dynamicFields) {
		this.dynamicFields = dynamicFields;
	}*/

	public static String getSameExecutionCheckbox() {
		return SAME_EXECUTION_CHECKBOX;
	}

	public static String getInfoInputSwitcher() {
		return INFO_INPUT_SWITCHER;
	}

	public static String getErrorLog() {
		return ERROR_LOG;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
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

	public String getImportToSameExecution() {
		return importToSameExecution;
	}

	public void setImportToSameExecution(String importToSameExecution) {
		this.importToSameExecution = importToSameExecution;
	}

	public String getFormatName(){
		return Endpoint.lookupByName(endpoint).getName();
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
        	
        	if(e.name().equals(Endpoint.lookupByName(endpoint).name())){
				bean.setFieldsConfiguration(dynamicFields);
				addImportToSameExecField(e, bean);
			}
        }
        return gson.toJson(formats);	
    }

	private void addImportToSameExecField(Endpoint e, FormatBean bean){
		if(Endpoint.JUNIT.equals(e)
				|| Endpoint.TESTNG.equals(e)
				|| Endpoint.NUNIT.equals(e)
				|| Endpoint.ROBOT.equals(e)){
			ParameterBean pb = new ParameterBean(SAME_EXECUTION_CHECKBOX, "same exec text box", false);
			pb.setConfiguration(dynamicFields.get(SAME_EXECUTION_CHECKBOX));
			bean.getConfigurableFields().add(0, pb);

		}
	}

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
	public void perform(@Nonnull Run<?,?> build,
						@Nonnull FilePath workspace,
						@Nonnull Launcher launcher,
						@Nonnull TaskListener listener)
			throws AbortException, InterruptedException, IOException {
		validate(dynamicFields);

		listener.getLogger().println("Starting import task...");

		listener.getLogger().println("Import Cucumber features Task started...");

        listener.getLogger().println("##########################################################");
        listener.getLogger().println("#### Importing the execution results to Xray  ####");
        listener.getLogger().println("##########################################################");
        XrayInstance serverInstance = ConfigurationUtils.getConfiguration(this.serverInstance);
        if(serverInstance == null){
        	throw new AbortException("The Jira server configuration of this task was not found.");
		}
		XrayImporter client = new XrayImporterImpl(serverInstance.getServerAddress(),
				serverInstance.getUsername(),
				serverInstance.getPassword());
		EnvVars env = build.getEnvironment(listener);
		String importFilePath = dynamicFields.get(com.xpandit.xray.model.DataParameter.FILEPATH.getKey());
		String resolved = this.expand(env,importFilePath);

		if(Endpoint.JUNIT.equals(this.endpoint)
				|| Endpoint.NUNIT.equals(this.endpoint)
				|| Endpoint.TESTNG.equals(this.endpoint)
				|| Endpoint.ROBOT.equals(this.endpoint)){
			UploadResult result;
			ObjectMapper mapper = new ObjectMapper();
			String key = null;
			for(FilePath fp : FileUtils.getFilePaths(workspace,resolved,listener)){
				result = uploadResults(workspace, listener,client, fp, env, key);
				if(key == null && "true".equals(importToSameExecution)){
					Map<String, Map> map = mapper.readValue(result.getMessage(), Map.class);
					key = (String) map.get("testExecIssue").get("key");
				}
			}
		} else{
			FilePath file = getFile(workspace, resolved, listener);
			uploadResults(workspace, listener, client, file, env, null);
		}

	}

	private UploadResult uploadResults(FilePath workspace,
									   TaskListener listener,
									   XrayImporter client,
									   FilePath resultsFile,
									   EnvVars env,
									   @Nullable String testExecutionKey) throws InterruptedException, IOException{
		try {
			Map<com.xpandit.xray.model.QueryParameter,String> queryParams = prepareQueryParam(env);

			if(StringUtils.isBlank(dynamicFields.get(com.xpandit.xray.model.QueryParameter.TEST_EXEC_KEY.getKey()))
					&& StringUtils.isNotBlank(testExecutionKey)
					&& "true".equals(importToSameExecution)){
				queryParams.put(com.xpandit.xray.model.QueryParameter.TEST_EXEC_KEY, testExecutionKey);
			}

			String importFilePath = dynamicFields.get(com.xpandit.xray.model.DataParameter.FILEPATH.getKey());
			String importInfo = dynamicFields.get(com.xpandit.xray.model.DataParameter.INFO.getKey());

			Map<com.xpandit.xray.model.DataParameter,Content> dataParams = new HashMap<>();

			if(StringUtils.isNotBlank(importFilePath)){
				Content results = new com.xpandit.xray.model.FileStream(resultsFile.getName(),resultsFile.read(),
						Endpoint.lookupByName(endpoint).getResultsMediaType());

				dataParams.put(com.xpandit.xray.model.DataParameter.FILEPATH, results);
			}
			if(StringUtils.isNotBlank(importInfo)){
				String resolved = this.expand(env,importInfo);
				String inputInfoSwitcher = dynamicFields.get(INFO_INPUT_SWITCHER);

				Content info;
				if(inputInfoSwitcher.equals("filePath")){
					FilePath infoFile = getFile(workspace,resolved,listener);
					info = new com.xpandit.xray.model.FileStream(infoFile.getName(),infoFile.read(),Endpoint.lookupByName(endpoint).getInfoFieldMediaType());
				}else{
					info = new com.xpandit.xray.model.StringContent(resolved, Endpoint.lookupByName(endpoint).getInfoFieldMediaType());
				}

				dataParams.put(com.xpandit.xray.model.DataParameter.INFO, info);
			}

			listener.getLogger().println("Starting to import results from " + resultsFile.getName() );
			UploadResult result = client.uploadResults(Endpoint.lookupByName(endpoint), dataParams, queryParams);
            listener.getLogger().println("response: " + result.getMessage());
			listener.getLogger().println("Sucessfully imported " + Endpoint.lookupByName(endpoint).getName() + " results from " + resultsFile.getName() );
			return result;

		}catch(XrayClientCoreGenericException e){
			LOG.error(ERROR_LOG, e);
			throw new AbortException(e.getMessage());
		}catch(XrayJenkinsGenericException e){
			LOG.error(ERROR_LOG, e);
			throw new AbortException(e.getMessage());
		}catch (IOException e) {
			LOG.error(ERROR_LOG, e);
			listener.error(e.getMessage());
			throw new IOException(e);
		}finally{
			client.shutdown();
		}
	}

	private Map<com.xpandit.xray.model.QueryParameter, String> prepareQueryParam(EnvVars env){
		Map<com.xpandit.xray.model.QueryParameter,String> queryParams = new EnumMap<>(QueryParameter.class);
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

		return queryParams;
	}

    private void validate(Map<String,String> dynamicFields) throws FormValidation{

		if(serverInstance == null){
			LOG.error("configuration id is null");
			throw new XrayJenkinsGenericException("configuration id is null");
		}
		if(endpoint == null){
			LOG.error("passed endpoint is null or could not be found");
			throw new XrayJenkinsGenericException("passed endpoint is null or could not be found");
		}
		if(dynamicFields.get("importFilePath") == null){
			LOG.error("importFilePath is null");
			throw new XrayJenkinsGenericException("importFilePath is null");
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
		public XrayImportBuilder newInstance(StaplerRequest req, JSONObject formData) throws Descriptor.FormException{
        	validateFormData(formData);

			Endpoint endpoint = Endpoint.lookupBySuffix(formData.getString("formatSuffix"));

			Map<String,String> fields = getDynamicFields(formData.getJSONObject("dynamicFields"));
            /*return new XrayImportBuilder(formData.getString(SERVER_INSTANCE),
                    endpoint,
                    getDynamicFields(formData.getJSONObject("dynamicFields")));*/
			/*(String serverInstance,
					String endpoint,
					String projectKey,
					String testEnvironments,
					String testPlanKey,
					String fixVersion,
					String importFilePath,
					String testExecKey,
					String revision,
					String importInfo,
					String inputInfoSwitcher,
					String importToSameExecution)*/

            return new XrayImportBuilder(
					(String)formData.get("serverInstance"),
					formData.getString("formatSuffix"),
					fields.get("projectKey"),
					(String)formData.get("testEnvironments"),
					fields.get("testPlanKey"),
					fields.get("fixVersion"),
					fields.get("importFilePath"),
					fields.get("testExecKey"),
					fields.get("revision"),
					fields.get("importInfo"),
					fields.get("inputInfoSwitcher"),
					fields.get("importToSameExecution"));


			
        }

        private void validateFormData(JSONObject formData) throws Descriptor.FormException{
			if(StringUtils.isBlank(formData.getString(SERVER_INSTANCE))){
				throw new Descriptor.FormException("Xray Results Import Task error, you must provide a valid JIRA Instance",SERVER_INSTANCE);
			}
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

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			LOG.info("applying XrayImportBuilder to following jobType class: {}", jobType.getSimpleName());
			return BuilderUtils.isSupportedJobType(jobType);
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
			return FormUtils.getServerInstanceItems();
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
            	addImportToSameExecField(e, bean);
            	formats.put(e.getSuffix(),bean);
            }
            return gson.toJson(formats);	
        }

        private void addImportToSameExecField(Endpoint e, FormatBean bean){
        	if(Endpoint.JUNIT.equals(e)
					|| Endpoint.TESTNG.equals(e)
					|| Endpoint.NUNIT.equals(e)
					|| Endpoint.ROBOT.equals(e)){
				ParameterBean pb = new ParameterBean(SAME_EXECUTION_CHECKBOX, "same exec text box", false);
				bean.getConfigurableFields().add(0, pb);
			}
		}

        public List<XrayInstance> getServerInstances() {
			return ServerConfiguration.get().getServerInstances();
		}

		public FormValidation doCheckServerInstance(){
			return ConfigurationUtils.anyAvailableConfiguration() ? FormValidation.ok() : FormValidation.error("No configured Server Instances found");
		}

    }

}
