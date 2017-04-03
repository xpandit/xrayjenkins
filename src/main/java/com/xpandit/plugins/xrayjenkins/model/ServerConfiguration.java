package com.xpandit.plugins.xrayjenkins.model;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import hudson.Extension;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

@Extension
public class ServerConfiguration extends GlobalConfiguration {
	
	private List<XrayInstance> serverInstances;
	
	public ServerConfiguration(){
		load();
	}
	
	@Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        
        req.bindJSON(this, formData.getJSONObject("xrayinstance"));
        
        save();
        return true;
    }
	
	public void setServerInstances(List<XrayInstance> serverInstances){
    	this.serverInstances = serverInstances;
    }
	
	public List<XrayInstance> getServerInstances(){
		return this.serverInstances;
	}
	
	public static ServerConfiguration get() {
	    return GlobalConfiguration.all().get(ServerConfiguration.class);
	}
	
	public FormValidation doTestConnection(@QueryParameter("serverAddress") final String serverAddress,
              @QueryParameter("username") final String username, @QueryParameter("password") final String password) throws IOException, ServletException {

          XrayInstance testXrayInstance = new XrayInstance(serverAddress,username,password);

          Boolean isConnectionOk = testXrayInstance.testConnection();
          if(isConnectionOk){
              return FormValidation.ok("Connection: Success!");
          }
          else{
              return FormValidation.error("Could not establish connection");
          }
      }
	

}
