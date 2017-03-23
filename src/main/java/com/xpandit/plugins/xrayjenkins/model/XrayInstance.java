/**
 * XP.RAVEN Project
 * <p>
 * Copyright (C) 2016 Xpand IT.
 * <p>
 * This software is proprietary.
 */
package com.xpandit.plugins.xrayjenkins.model;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

/**
 * Represents a JIRA/Xray instance.
 */
public class XrayInstance {
	
	private static String AUTHORIZATION_HEADER_PREFIX = "Basic ";
	
    private final String serverAddress;
    private final String username;
    private final String password;
    private CloseableHttpClient httpclient;

    public XrayInstance(String serverAddress, String username, String password) {
        this.serverAddress = serverAddress;
        this.username = username;
        this.password = password;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
    
    
    public Boolean testConnection(){
    	createHttpClient();
        HttpGet get = new HttpGet(this.getServerAddress());
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
    
    private void createHttpClient() {

    	String basicAuthorizationHeaderValue = getBasicAuthorizationHeaderValue(this.getUsername(),this.getPassword());
        Header header = new BasicHeader("Authorization", basicAuthorizationHeaderValue);

        List<Header> headers = new ArrayList<>();
        headers.add(header);
        
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(), SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            this.httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).setDefaultHeaders(headers).build();
        } catch (KeyManagementException e1) {
            e1.printStackTrace();
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (KeyStoreException e1) {
            e1.printStackTrace();
        }
    }
    
    
    private static String getBasicAuthorizationHeaderValue(String userName, String password) {
        byte[] bytesEncoded = Base64.encodeBase64((userName + ":" + password).getBytes());
        String authorizationHeader = AUTHORIZATION_HEADER_PREFIX + new String(bytesEncoded);
        return authorizationHeader;
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

