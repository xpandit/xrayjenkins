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

	private static final String SAME_EXECUTION_CHECKBOX = "importToSameExecution";
	private static final String INFO_INPUT_SWITCHER = "inputInfoSwitcher";
	private static final String SERVER_INSTANCE = "serverInstance";
	private static final String ERROR_LOG = "Error while performing import tasks";
	private static final String TEST_ENVIRONMENTS = "testEnvironments";
	private static final String PROJECT_KEY = "projectKey";
	private static final String TEST_PLAN_KEY = "testPlanKey";
	private static final String FIX_VERSION = "fixVersion";
	private static final String IMPORT_FILE_PATH = "importFilePath";
	private static final String TEST_EXEC_KEY = "testExecKey";
	private static final String REVISION_FIELD = "revision";
	private static final String IMPORT_INFO = "importInfo";
	private static final String FORMAT_SUFFIX = "formatSuffix";

    private String formatSuffix; //value of format select
    private String serverInstance;//Configuration ID of the JIRA instance
    private String inputInfoSwitcher;//value of the input type switcher
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

	/**
	 * This constructor is compatible with pipelines projects
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
    	this.endpoint = endpoint;
        this.formatSuffix = lookupForEndpoint() != null ? lookupForEndpoint().getSuffix() : null;
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
	}

	private Map<String,String> getDynamicFieldsMap(){

		Map<String,String> dynamicFields = new HashMap<>();

    	if (!StringUtils.isBlank(projectKey)){
    		dynamicFields.put(PROJECT_KEY,projectKey);
		}
		if (!StringUtils.isBlank(testEnvironments)){
			dynamicFields.put(TEST_ENVIRONMENTS,testEnvironments);
		}
		if (!StringUtils.isBlank(testPlanKey)){
			dynamicFields.put(TEST_PLAN_KEY,testPlanKey);
		}
		if (!StringUtils.isBlank(fixVersion)){
			dynamicFields.put(FIX_VERSION,fixVersion);
		}
		if (!StringUtils.isBlank(importFilePath)){
			dynamicFields.put(IMPORT_FILE_PATH,importFilePath);
		}
		if (!StringUtils.isBlank(testExecKey)){
			dynamicFields.put(TEST_EXEC_KEY,testExecKey);
		}
		if (!StringUtils.isBlank(projectKey)){
			dynamicFields.put(REVISION_FIELD,revision);
		}
		if(!StringUtils.isBlank(importInfo)){
    		dynamicFields.put(IMPORT_INFO, importInfo);
		}
		if(!StringUtils.isBlank(inputInfoSwitcher)){
    		dynamicFields.put(INFO_INPUT_SWITCHER, inputInfoSwitcher);
		}
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

	public static String getInfoInputSwitcher() {
		return INFO_INPUT_SWITCHER;
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
        Map<String,FormatBean> formats = new HashMap<>();
        for(Endpoint e : Endpoint.values()){
        	FormatBean bean = e.toBean();
        	formats.put(e.getSuffix(),bean);
        	if(e.name().equals(lookupForEndpoint().name())){
				bean.setFieldsConfiguration(getDynamicFieldsMap());
				addImportToSameExecField(e, bean);
			}
        }
        return gson.toJson(formats);	
    }

    /**
     * Using the browser interface, we will receive the endpoint suffix
     * but in pipeline projects the user must be able to also use the endpoint name
     * @return the matching <code>Endpoint</code> or <code>null</code> if not found.
     */
    @Nullable
    private Endpoint lookupForEndpoint(){
		Endpoint targetedEndpoint = Endpoint.lookupByName(endpoint);
		return targetedEndpoint != null ? targetedEndpoint : Endpoint.lookupBySuffix(endpoint);
	}

	private void addImportToSameExecField(Endpoint e, FormatBean bean){
		if(Endpoint.JUNIT.equals(e)
				|| Endpoint.TESTNG.equals(e)
				|| Endpoint.NUNIT.equals(e)
				|| Endpoint.ROBOT.equals(e)){
			ParameterBean pb = new ParameterBean(SAME_EXECUTION_CHECKBOX, "same exec text box", false);
			pb.setConfiguration(this.importToSameExecution);
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
			throws InterruptedException, IOException {
		validate(getDynamicFieldsMap());

		listener.getLogger().println("Starting import task...");

		listener.getLogger().println("Import Cucumber features Task started...");

        listener.getLogger().println("##########################################################");
        listener.getLogger().println("#### Importing the execution results to Xray  ####");
        listener.getLogger().println("##########################################################");
        XrayInstance xrayInstance = ConfigurationUtils.getConfiguration(this.serverInstance);
        if(xrayInstance == null){
        	throw new AbortException("The Jira server configuration of this task was not found.");
		}
		XrayImporter client = new XrayImporterImpl(xrayInstance.getServerAddress(),
                xrayInstance.getUsername(),
                xrayInstance.getPassword());
		EnvVars env = build.getEnvironment(listener);
		String resolved = this.expand(env,this.importFilePath);

		Endpoint endpointValue = Endpoint.lookupBySuffix(this.endpoint);
		if(Endpoint.JUNIT.equals(endpointValue)
				|| Endpoint.NUNIT.equals(endpointValue)
				|| Endpoint.TESTNG.equals(endpointValue)
				|| Endpoint.ROBOT.equals(endpointValue)){
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

    /**
     * Upload the results to the xray instance
     * @param workspace the Workspace
     * @param listener the TaskListener
     * @param client the xray client
     * @param resultsFile the FilePath of the results file
     * @param env the environment variables
     * @param sameTestExecutionKey The key used when multiple results are imported to the same Test Execution
     * @return the upload results
     */
	private UploadResult uploadResults(FilePath workspace,
									   TaskListener listener,
									   XrayImporter client,
									   FilePath resultsFile,
									   EnvVars env,
									   @Nullable String sameTestExecutionKey) throws InterruptedException, IOException{
		try {
		    Endpoint targetEndpoint = lookupForEndpoint();
			Map<com.xpandit.xray.model.QueryParameter,String> queryParams = prepareQueryParam(env);

			if(StringUtils.isBlank(this.testExecKey)
					&& StringUtils.isNotBlank(sameTestExecutionKey)
					&& "true".equals(importToSameExecution)){
				queryParams.put(com.xpandit.xray.model.QueryParameter.TEST_EXEC_KEY, sameTestExecutionKey);
			}

			Map<com.xpandit.xray.model.DataParameter,Content> dataParams = new HashMap<>();

			if(StringUtils.isNotBlank(this.importFilePath)){
				Content results = new com.xpandit.xray.model.FileStream(resultsFile.getName(),resultsFile.read(),
                        targetEndpoint.getResultsMediaType());

				dataParams.put(com.xpandit.xray.model.DataParameter.FILEPATH, results);
			}
			if(StringUtils.isNotBlank(this.importInfo)){
				String resolved = this.expand(env,this.importInfo);

				Content info;
				if(this.inputInfoSwitcher.equals("filePath")){
					FilePath infoFile = getFile(workspace,resolved,listener);
					info = new com.xpandit.xray.model.FileStream(infoFile.getName(),infoFile.read(),targetEndpoint.getInfoFieldMediaType());
				}else{
					info = new com.xpandit.xray.model.StringContent(resolved, targetEndpoint.getInfoFieldMediaType());
				}

				dataParams.put(com.xpandit.xray.model.DataParameter.INFO, info);
			}

			listener.getLogger().println("Starting to import results from " + resultsFile.getName() );
			UploadResult result = client.uploadResults(targetEndpoint, dataParams, queryParams);
            listener.getLogger().println("response: " + result.getMessage());
			listener.getLogger().println("Sucessfully imported " + targetEndpoint.getName() + " results from " + resultsFile.getName() );
			return result;

		}catch(XrayClientCoreGenericException | XrayJenkinsGenericException e){
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
		queryParams.put(com.xpandit.xray.model.QueryParameter.PROJECT_KEY, this.expand(env,this.projectKey));
		queryParams.put(com.xpandit.xray.model.QueryParameter.TEST_EXEC_KEY, this.expand(env,this.testExecKey));
		queryParams.put(com.xpandit.xray.model.QueryParameter.TEST_PLAN_KEY, this.expand(env,this.testPlanKey));
		queryParams.put(com.xpandit.xray.model.QueryParameter.TEST_ENVIRONMENTS, this.expand(env,this.testEnvironments));
		queryParams.put(com.xpandit.xray.model.QueryParameter.REVISION, this.expand(env,this.revision));
		queryParams.put(com.xpandit.xray.model.QueryParameter.FIX_VERSION, this.expand(env,this.fixVersion));
		return queryParams;
	}

    private void validate(Map<String,String> dynamicFields) throws FormValidation{

		if(serverInstance == null){
			LOG.error("configuration id is null");
			throw new XrayJenkinsGenericException("configuration id is null");
		}
		if(endpoint == null || lookupForEndpoint() == null){
			LOG.error("passed endpoint is null or could not be found");
			throw new XrayJenkinsGenericException("passed endpoint is null or could not be found");
		}
		if(this.importFilePath == null){
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

      	 if(this.importFilePath.contains("../")){
             throw FormValidation.error("You cannot provide file paths for upper directories.");
         }
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
			Map<String,String> fields = getDynamicFields(formData.getJSONObject("dynamicFields"));
            return new XrayImportBuilder(
					(String)formData.get(SERVER_INSTANCE),
					formData.getString(FORMAT_SUFFIX),
					fields.get(PROJECT_KEY),
					fields.get(TEST_ENVIRONMENTS),
					fields.get(TEST_PLAN_KEY),
					fields.get(FIX_VERSION),
					fields.get(IMPORT_FILE_PATH),
					fields.get(TEST_EXEC_KEY),
					fields.get(REVISION_FIELD),
					fields.get(IMPORT_INFO),
					fields.get(INFO_INPUT_SWITCHER),
					fields.get(SAME_EXECUTION_CHECKBOX));
        }

        private void validateFormData(JSONObject formData) throws Descriptor.FormException{
			if(StringUtils.isBlank(formData.getString(SERVER_INSTANCE))){
				throw new Descriptor.FormException("Xray Results Import Task error, you must provide a valid JIRA Instance",SERVER_INSTANCE);
			}
		}

        private Map<String,String> getDynamicFields(JSONObject configuredFields){
        	
        	Map<String,String> dynamicFields = new HashMap<>();
        	
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
            Map<String,FormatBean> formats = new HashMap<>();
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
