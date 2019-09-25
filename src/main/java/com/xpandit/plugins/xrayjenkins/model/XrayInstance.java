/**
 * XP.RAVEN Project
 * <p>
 * Copyright (C) 2016 Xpand IT.
 * <p>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.model;

import com.xpandit.plugins.xrayjenkins.exceptions.XrayJenkinsGenericException;
import hudson.model.Run;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Represents a JIRA/Xray instance.
 */
public class XrayInstance {
	
	private String configID;
	private String alias;
	private String serverAddress;
	private HostingType hosting;
    private String credentialId;
    private CredentialResolver credentialResolver;

    public XrayInstance(String serverAddress, String hosting, String credentialId) {
    	this.configID =  "";
    	this.alias = serverAddress;
        this.serverAddress = serverAddress;

		this.hosting = HostingType.findByName(hosting);

		if (this.hosting == null) {
			throw new XrayJenkinsGenericException("Hosting type not recognized");
		}

        this.credentialId = credentialId;
    }

	@DataBoundConstructor
 	public XrayInstance(String configID, String alias , String hosting, String serverAddress, String credentialId) {
    	this(serverAddress, hosting, credentialId);

		this.configID = StringUtils.isBlank(configID) ? UUID.randomUUID().toString() : configID;
 		this.alias = alias;
 		
 	}

    public String getConfigID(){
			return this.configID;
		}
		
	public void setConfigID(String configID){
		this.configID = configID;
	}
		
	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	@Nonnull
	public CredentialResolver getCredential(final Run<?, ?> runContext) {
		this.credentialResolver = ObjectUtils.defaultIfNull(this.credentialResolver, new CredentialResolver(this.credentialId, runContext));
		return this.credentialResolver;
	}
	
	public HostingType getHosting() { return hosting; }

	public void setHosting(HostingType hosting) { this.hosting = hosting; }

	public String getCredentialId() {
		return credentialId;
	}

	public void setCredentialId(String credentialId) {
		this.credentialId = credentialId;
	}
}

