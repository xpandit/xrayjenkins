/**
 * XP.RAVEN Project
 * <p>
 * Copyright (C) 2016 Xpand IT.
 * <p>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.model;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.xpandit.xray.service.impl.XrayClientImpl;
import com.xpandit.xray.service.impl.XrayCloudClientImpl;

import hudson.Extension;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

@Extension
public class ServerConfiguration extends GlobalConfiguration {
	
	private List<XrayInstance> serverInstances;
	private static final String CLOUD_URL = "https://xray.cloud.xpand-it.com";
	
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
	
	public FormValidation doTestConnection(@QueryParameter("hosting") final String hosting,
	                                       @QueryParameter("serverAddress") final String serverAddress,
                                           @QueryParameter("username") final String username,
                                           @QueryParameter("password") final String password) throws IOException, ServletException {

        if(serverAddress == null){
            return FormValidation.error("Server Address null");
        }

	    if(username == null){
            return FormValidation.error("Username null");
        }

        if(password == null){
            return FormValidation.error("Password null");
        }

        if(hosting == null){
            return FormValidation.error("Hosting null");
        }

        Boolean isConnectionOk;

        if(hosting.equals("cloud"))
            isConnectionOk = (new XrayCloudClientImpl(CLOUD_URL,username,password)).testConnection();
        else
            isConnectionOk = (new XrayClientImpl(serverAddress,username,password)).testConnection();

        if(isConnectionOk)
            return FormValidation.ok("Connection: Success!");
        else
            return FormValidation.error("Could not establish connection");
  }
}
