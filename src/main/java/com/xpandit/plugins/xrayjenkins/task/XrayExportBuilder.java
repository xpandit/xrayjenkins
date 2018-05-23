/**
 * XP.RAVEN Project
 * <p>
 * Copyright (C) 2016 Xpand IT.
 * <p>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.task;

import com.xpandit.plugins.xrayjenkins.Utils.ConfigurationUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import com.xpandit.plugins.xrayjenkins.model.ServerConfiguration;
import com.xpandit.plugins.xrayjenkins.model.XrayInstance;
import com.xpandit.xray.exception.XrayClientCoreGenericException;
import com.xpandit.xray.service.XrayExporter;
import com.xpandit.xray.service.impl.XrayExporterImpl;
import hudson.AbortException;
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
public class XrayExportBuilder extends Builder implements SimpleBuildStep {

    private Map<String,String> fields;
    
    private String serverInstance;//Configuration ID of the JIRA instance
    private String issues;
    private String filter;
    private String filePath;

    public XrayExportBuilder(String serverInstance,  Map<String, String> fields) {
    	this.fields = fields;
    	this.issues = fields.get("issues");
    	this.filter = fields.get("filter");
    	this.filePath = fields.get("filePath");
    	this.serverInstance = serverInstance;
	}
   
    @Override
    public void perform(Run<?,?> build,
                        FilePath workspace,
                        Launcher launcher,
                        TaskListener listener) throws AbortException, IOException {
        
        listener.getLogger().println("Starting export task...");
        
        listener.getLogger().println("##########################################################");
        listener.getLogger().println("####   Xray for JIRA is exporting the feature files  ####");
        listener.getLogger().println("##########################################################");


        XrayInstance serverInstance = ConfigurationUtils.getConfiguration(this.serverInstance);
        XrayExporter client = new XrayExporterImpl(serverInstance.getServerAddress(),
                serverInstance.getUsername(),
                serverInstance.getPassword());
        
        try{

            if (StringUtils.isNotBlank(issues)) 
                listener.getLogger().println("Issues: "+issues);

            if (StringUtils.isNotBlank(filter)) 
                listener.getLogger().println("Filter: "+filter);

            if (StringUtils.isNotBlank(filePath)) 
                listener.getLogger().println("Will save the feature files in: "+filePath);
           
            InputStream file = client.downloadFeatures(issues,filter,"true");
            this.unzipFeatures(listener, workspace, filePath, file);
            listener.getLogger().println("Sucessfully exported the Cucumber features");
        }catch (XrayClientCoreGenericException e) {
            e.printStackTrace();
            throw new AbortException(e.getMessage());
		}catch (IOException e) {
            e.printStackTrace();
            listener.error(e.getMessage());
            throw new IOException(e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            listener.error(e.getMessage());
            throw new AbortException(e.getMessage());
        }
    }
    
    private void unzipFeatures(TaskListener listener, FilePath workspace, String filePath, InputStream zip) throws IOException, InterruptedException {

        if (StringUtils.isBlank(filePath)) {
            filePath = "features/";
        }

        FilePath outputFile = new FilePath(workspace, filePath.trim());
        listener.getLogger().println("###################### Unzipping file ####################");
        outputFile.mkdirs();
        outputFile.unzipFrom(zip);
        listener.getLogger().println("###################### Unzipped file #####################");
    }

    
    public String getServerInstance() {
		return serverInstance;
	}

	public void setServerInstance(String serverInstance) {
		this.serverInstance = serverInstance;
	}


	public String getIssues() {
		return issues;
	}

	public void setIssues(String issues) {
		this.issues = issues;
	}


	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}


	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}


	public Map<String,String> getFields() {
		return fields;
	}

	public void setFields(Map<String,String> fields) {
		this.fields = fields;
	}

	@Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {

        public Descriptor() {
        	super(XrayExportBuilder.class);
            load();
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
           
        	save();
            return true;
            
        }
        
        @Override
		public XrayExportBuilder newInstance(StaplerRequest req, JSONObject formData){
			return new XrayExportBuilder(formData.getString("serverInstance"),
                    getFields(formData.getJSONObject("fields")));

        }
        
        
        public ListBoxModel doFillServerInstanceItems() {
        	
            ListBoxModel items = new ListBoxModel();
            List<XrayInstance> serverInstances =  getServerInstances();
            for(XrayInstance sc : serverInstances)
            	items.add(sc.getAlias(),sc.getConfigID());
            
            return items;
        }
        
        private XrayInstance getConfiguration(String configID) {
        	XrayInstance config =  null;
        	List<XrayInstance> serverInstances =  getServerInstances();
        	for(XrayInstance sc : serverInstances){
        		if(sc.getConfigID().equals(configID)){
        			config = sc;break;
        		}
        	}
        	return config;
		}
        

        private Map<String, String> getFields(JSONObject configuredFields) {
        	Map<String,String> fields = new HashMap<String,String>();
        	
        	Set<String> keys = configuredFields.keySet();
        	
        	for(String key : keys){
        		if(configuredFields.containsKey(key)){
        			String value = configuredFields.getString(key);
					if(StringUtils.isNotBlank(value))
						fields.put(key, value);
        		}
        	}
        	
        	return fields;
		}
		
		@Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return FreeStyleProject.class.isAssignableFrom(jobType);
        }

        @Override
        public String getDisplayName() {
            return "Xray: Cucumber Features Export Task";
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
        
        
        public List<XrayInstance> getServerInstances() {
			return ServerConfiguration.get().getServerInstances();
		}
        
    }

}
