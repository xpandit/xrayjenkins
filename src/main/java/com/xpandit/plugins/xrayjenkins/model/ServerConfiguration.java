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
import org.apache.commons.lang3.StringUtils;

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

	public String getCloudHostingType(){
	    return XrayInstance.HostingType.CLOUD.value;
    }

    public String getServerHostingType(){
        return XrayInstance.HostingType.SERVER.value;
    }
	
	public static ServerConfiguration get() {
	    return GlobalConfiguration.all().get(ServerConfiguration.class);
	}
	
	public FormValidation doTestConnection(@QueryParameter("hosting") final String hosting,
	                                       @QueryParameter("serverAddress") final String serverAddress,
                                           @QueryParameter("username") final String username,
                                           @QueryParameter("password") final String password) throws IOException, ServletException {


        if(StringUtils.isBlank(username) || StringUtils.isBlank(password)){
            return FormValidation.error("Authentication not filled!");
        }

        if(StringUtils.isBlank(hosting)){
            return FormValidation.error("Hosting type can't be blank.");
        }

        boolean isConnectionOk;

        if(hosting.equals(XrayInstance.HostingType.CLOUD.value)) {
            isConnectionOk = (new XrayCloudClientImpl(username, password)).testConnection();
        } else if(hosting.equals(XrayInstance.HostingType.SERVER.value)) {
            if(StringUtils.isBlank(serverAddress)) {
                return FormValidation.error("Server address can't be empty");
            }
            isConnectionOk = (new XrayClientImpl(serverAddress, username, password)).testConnection();
        } else {
            return FormValidation.error("Hosting type not recognized.");
        }

        if(isConnectionOk) {
            return FormValidation.ok("Connection: Success!");
        } else {
            return FormValidation.error("Could not establish connection.");
        }
    }
}
