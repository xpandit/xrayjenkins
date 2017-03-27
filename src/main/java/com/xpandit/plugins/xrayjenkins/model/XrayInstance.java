/**
 * XP.RAVEN Project
 * <p>
 * Copyright (C) 2016 Xpand IT.
 * <p>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.model;

import java.io.IOException;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Represents a JIRA/Xray instance.
 */
public class XrayInstance {
	
	private String configID;
	private String alias;
	private String serverAddress;
    private String username;
    private String password;
    private CloseableHttpClient httpclient;

    public XrayInstance(String serverAddress, String username, String password) {
    	this.configID =  "";
    	this.alias = serverAddress;
        this.serverAddress = serverAddress;
        this.username = username;
        this.password = password;
    }
    
    @DataBoundConstructor
 	public XrayInstance(String configID, String alias,String serverAddress,
 			String username,String password){
 		
    	this(serverAddress,username,password);
    	
    	
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
    
    public Boolean testConnection(){
        HttpGet get = prepareHTTPGET();
        HttpResponse response;
        try {
            response = this.httpclient.execute(get);
            int statusCode = response.getStatusLine().getStatusCode();
            
            if(statusCode != 200){
                return false;
            }
            else{
                return true;
            }
        } catch (IOException e) {
            return false;
        } finally{
        	destroyClient();
        }
    }
    
    private HttpGet prepareHTTPGET() {
    	 HttpGet get = new HttpGet(this.getServerAddress());
    	 this.httpclient = new DefaultHttpClient();
         String encoding = Base64.encodeBase64String((username + ":" + password).getBytes());
         get.setHeader("Authorization", "Basic " + encoding);     
         return get;
    }
    
    
    public void destroyClient() {
        if (this.httpclient != null) {
            try {
                this.httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    
    
}

