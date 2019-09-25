/**
 * XP.RAVEN Project
 * <p>
 * Copyright (C) 2016 Xpand IT.
 * <p>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.model;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import org.apache.commons.collections.CollectionUtils;
import org.kohsuke.stapler.AncestorInPath;
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
	
	private List<XrayInstance> serverInstances = new ArrayList<>();
	
	public ServerConfiguration(){
		load();
        checkForCompatibility();
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

	public String getCloudHostingTypeName(){
	    return HostingType.getCloudHostingTypeName();
    }

    public String getServerHostingTypeName(){
        return HostingType.getServerHostingTypeName();
    }
	
	public static ServerConfiguration get() {
	    return GlobalConfiguration.all().get(ServerConfiguration.class);
	}

    public ListBoxModel doFillCredentialIdItems(@AncestorInPath Item item, @QueryParameter String credentialId) {
        
        final StandardListBoxModel result = new StandardListBoxModel();

        final List<StandardUsernamePasswordCredentials> credentials = getAllCredentials(item);

        for (StandardUsernamePasswordCredentials credential : credentials) {
            result.with(credential);
        }
        return result.includeCurrentValue(credentialId);
    }

    public FormValidation doCheckCredentialId(
            @AncestorInPath Item item,
            @QueryParameter String value
    ) {
        if (item == null) {
            if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.ok();
            }
        } else {
            if (!item.hasPermission(Item.EXTENDED_READ)
                    && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return FormValidation.ok(); // (3)
            }
        }
        if (StringUtils.isBlank(value)) { // (4)
            return FormValidation.ok(); // (4)
        }
        
        if (!credentialExists(item, value)) {
            return FormValidation.error("Cannot find currently selected credentials");
        }
        return FormValidation.ok();
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

        if(StringUtils.isBlank(hosting)) {
            return FormValidation.error("Hosting type can't be blank.");
        } else if(hosting.equals(HostingType.CLOUD.getTypeName())) {
            isConnectionOk = (new XrayCloudClientImpl(username, password)).testConnection();
        } else if(hosting.equals(HostingType.SERVER.getTypeName())) {
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

    private void checkForCompatibility(){
        for(XrayInstance instance : serverInstances){
            if(instance.getHosting() == null){
                instance.setHosting(HostingType.getDefaultType());
            }
        }
    }

    private List<StandardUsernamePasswordCredentials> getAllCredentials(@Nullable final Item item) {
        final List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(
                StandardUsernamePasswordCredentials.class,
                item,
                ACL.SYSTEM,
                Collections.<DomainRequirement>emptyList());
        
        if (CollectionUtils.isEmpty(credentials)) {
            return Collections.emptyList();
        }
        return credentials;
    }

    private boolean credentialExists(@Nullable final Item item, @Nullable final String credentialId) {
        if (StringUtils.isNotBlank(credentialId)) {
            final List<StandardUsernamePasswordCredentials> credentials = getAllCredentials(item);
            for (StandardUsernamePasswordCredentials credential : credentials) {
                if (StringUtils.equals(credential.getId(), credentialId)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
