/**
 * XP.RAVEN Project
 * <p>
 * Copyright (C) 2016 Xpand IT.
 * <p>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.model;

import java.util.UUID;

import com.xpandit.plugins.xrayjenkins.exceptions.XrayJenkinsGenericException;
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
    private String username;
    private String password;

    public XrayInstance(String serverAddress, String hosting, String username, String password) {
    	this.configID =  "";
    	this.alias = serverAddress;
        this.serverAddress = serverAddress;

        switch (hosting){
			case "cloud":
				this.hosting = HostingType.CLOUD;
				break;
			case "server":
				this.hosting = HostingType.SERVER;
				break;
			default:
				throw new XrayJenkinsGenericException("Hosting type not recognized");
		}

        this.username = username;
        this.password = password;
    }

	@DataBoundConstructor
 	public XrayInstance(String configID, String alias , String hosting, String serverAddress, String username, String password){

    	this(serverAddress, hosting, username, password);
    	
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

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public HostingType getHosting() { return hosting; }

	public void setHosting(HostingType hosting) { this.hosting = hosting; }
}

